package dev.santiagosantofimio.worldomens.event.impl;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldomens.WorldOmensMod;
import dev.santiagosantofimio.worldomens.config.EventSettings;
import dev.santiagosantofimio.worldomens.event.ActiveEvent;
import dev.santiagosantofimio.worldomens.event.EventManager;
import dev.santiagosantofimio.worldomens.event.WorldEvent;
import dev.santiagosantofimio.worldomens.i18n.Lang;
import dev.santiagosantofimio.worldomens.item.EventItems;
import dev.santiagosantofimio.worldomens.mob.Creature;
import dev.santiagosantofimio.worldomens.mob.EclipseWraith;
import dev.santiagosantofimio.worldomens.mob.MobSpawning;
import dev.santiagosantofimio.worldomens.util.Advancements;
import dev.santiagosantofimio.worldomens.util.DayCycle;
import dev.santiagosantofimio.worldomens.util.DeferredDiscard;
import dev.santiagosantofimio.worldomens.util.Entities;
import dev.santiagosantofimio.worldomens.platform.Hooks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * An eclipse, in the daytime: the sky goes dark, monsters walk in the sun without burning, phantoms dive from the
 * sky and the eclipse wraith comes for the players. When the sun comes back, the monsters it brought burn again.
 */
public final class EclipseEvent implements WorldEvent {
	public static final String ID = "eclipse";
	/** Mobs the eclipse brought, which the sun spares while it lasts. */
	public static final String SHADED_TAG = WorldOmensMod.MOD_ID + ".shaded";
	private static final String WRAITH_SPAWNED = "wraithSpawned";
	private static final long EARLIEST_START = 1000;
	private static final long LATEST_START = 8000;
	private static final List<EntityType<? extends Mob>> MONSTERS = List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER);
	private static final int GLOW_RANGE = 32;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public JsonObject defaultSettings() {
		// At most once every 6 in-game days; lasts 5 minutes.
		JsonObject json = EventSettings.defaults(true, 6, 7200, 300);
		json.addProperty("spawnInterval", 15);
		json.addProperty("monstersPerPlayer", 2);
		json.addProperty("phantomInterval", 30);
		json.addProperty("wraith", true);
		json.addProperty("wraithDelay", 60);
		json.addProperty("wraithHealth", 150.0);
		json.addProperty("shardChance", 0.5);
		return json;
	}

	@Override
	public BossEvent.BossBarColor barColor() {
		return BossEvent.BossBarColor.PURPLE;
	}

	@Override
	public ChatFormatting textColor() {
		return ChatFormatting.DARK_PURPLE;
	}

	@Override
	public SoundEvent startSound() {
		return SoundEvents.ELDER_GUARDIAN_CURSE;
	}

	@Override
	public boolean darkenSky() {
		return true;
	}

	@Override
	public boolean isAvailable(ServerLevel level) {
		return level.dimension() == Level.OVERWORLD && DayCycle.hasDayCycle(level) && level.getDifficulty() != Difficulty.PEACEFUL;
	}

	@Override
	public boolean canStart(ServerLevel level) {
		return DayCycle.isBetween(level, EARLIEST_START, LATEST_START);
	}

	@Override
	public void tick(ServerLevel level, ActiveEvent active) {
		EventSettings settings = settings();
		EclipseState state = active.state(EclipseState::new);
		int elapsed = (active.totalTicks() - active.remainingTicks()) / 20;
		state.shaded.removeIf(mob -> mob.removed || !mob.isAlive());
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) {
				continue;
			}
			level.sendParticles(player, ParticleTypes.ASH, false, player.getX(), player.getY() + 2.0, player.getZ(), 30, 8.0, 3.0, 8.0, 0.0);
			if (player.isCreative()) {
				continue;
			}
			if (elapsed > 0 && elapsed % Math.max(1, settings.getInt("spawnInterval")) == 0) {
				for (int i = 0; i < settings.getInt("monstersPerPlayer"); i++) {
					Mob mob = spawnMonster(level, player.blockPosition());
					if (mob != null) {
						state.shaded.add(mob);
					}
				}
			}
			if (elapsed > 0 && elapsed % Math.max(1, settings.getInt("phantomInterval")) == 0) {
				Mob phantom = spawnPhantom(level, player.blockPosition());
				if (phantom != null) {
					state.shaded.add(phantom);
				}
			}
		}
		tickWraith(level, active, state, elapsed);
	}

	@Override
	public void fastTick(ServerLevel level, ActiveEvent active) {
		for (Mob mob : active.state(EclipseState::new).shaded) {
			keepShaded(mob);
		}
	}

	@Override
	public void end(ServerLevel level, ActiveEvent active) {
		EclipseState state = active.state(EclipseState::new);
		// The sun is back: what the eclipse brought burns like any other monster.
		for (Mob mob : state.shaded) {
			if (!mob.removed) {
				mob.removeEffect(MobEffects.FIRE_RESISTANCE);
				mob.removeTag(SHADED_TAG);
			}
		}
		state.shaded.clear();
		if (state.wraith != null && !state.wraith.removed) {
			level.sendParticles(ParticleTypes.SQUID_INK, state.wraith.getX(), state.wraith.getY(), state.wraith.getZ(), 60, 2.0, 1.0, 2.0, 0.05);
			Creature.remove(state.wraith);
			for (ServerPlayer player : level.players()) {
				player.sendMessage(Lang.component(player, "mob.eclipse_wraith.retreat").withStyle(textColor()), net.minecraft.Util.NIL_UUID);
			}
		}
		state.wraith = null;
	}

	/** Spares {@code mob} from the sun: fire resistance, hidden, and no flames. */
	public static void shadeFromSun(Mob mob) {
		mob.addTag(SHADED_TAG);
		mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
	}

	/** No flames on a shaded mob: the sun lights it again every tick. */
	public static void keepShaded(Mob mob) {
		if (mob.isOnFire()) {
			mob.clearFire();
		}
	}

	/** A zombie, skeleton or spider out of the dark near {@code center}, spared by the sun. */
	public static Mob spawnMonster(ServerLevel level, BlockPos center) {
		EntityType<? extends Mob> type = MONSTERS.get(level.getRandom().nextInt(MONSTERS.size()));
		BlockPos spot = MobSpawning.spotNear(level, type, center, 10, 24, 6, 10);
		if (spot == null) {
			return null;
		}
		Mob mob = type.create(level);
		if (mob == null) {
			return null;
		}
		mob.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.EVENT, null, null);
		shadeFromSun(mob);
		level.addFreshEntity(mob);
		level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + 1.0, mob.getZ(), 15, 0.3, 0.6, 0.3, 0.02);
		return mob;
	}

	/** A phantom high above {@code center}, spared by the sun. */
	public static Mob spawnPhantom(ServerLevel level, BlockPos center) {
		BlockPos at = center.offset(level.getRandom().nextInt(17) - 8, 18 + level.getRandom().nextInt(6), level.getRandom().nextInt(17) - 8);
		if (!Entities.isTicking(level, at) || !level.getBlockState(at).isAir()) {
			return null;
		}
		Phantom phantom = EntityType.PHANTOM.create(level);
		if (phantom == null) {
			return null;
		}
		phantom.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		phantom.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
		shadeFromSun(phantom);
		level.addFreshEntity(phantom);
		return phantom;
	}

	/** Brings the wraith once the eclipse is under way. Its boss bar and pulses are its own. */
	private void tickWraith(ServerLevel level, ActiveEvent active, EclipseState state, int elapsed) {
		EventSettings settings = settings();
		if (settings.getBoolean("wraith") && !active.data().getBoolean(WRAITH_SPAWNED) && elapsed >= settings.getInt("wraithDelay")) {
			List<ServerPlayer> players = new ArrayList<>();
			for (ServerPlayer player : level.players()) {
				if (!player.isSpectator() && !player.isCreative()) {
					players.add(player);
				}
			}
			if (!players.isEmpty()) {
				ServerPlayer target = players.get(level.getRandom().nextInt(players.size()));
				BlockPos at = target.blockPosition().above(16);
				if (Entities.isTicking(level, at) && spawnWraith(level, active, at, Lang.language(target)) != null) {
					level.playSound(null, at, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5f, 0.6f);
					for (ServerPlayer player : level.players()) {
						player.sendMessage(Lang.component(player, "mob.eclipse_wraith.arrived").withStyle(textColor()), net.minecraft.Util.NIL_UUID);
					}
				}
			}
		}
		if (state.wraith != null && (state.wraith.removed || !state.wraith.isAlive())) {
			state.wraith = null;
		}
	}

	/** Spawns the wraith at {@code pos} now, for tests and admins. */
	public Vex spawnWraith(ServerLevel level, ActiveEvent active, BlockPos pos, String language) {
		Vex wraith = EclipseWraith.spawn(level, pos, language, settings().getDouble("wraithHealth"));
		if (wraith != null) {
			active.state(EclipseState::new).wraith = wraith;
			active.data().putBoolean(WRAITH_SPAWNED, true);
		}
		return wraith;
	}

	/** The eclipse shard: night vision for its holder, and every monster around glows for a while. */
	public static int useShard(ServerLevel level, ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0));
		int revealed = 0;
		for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(GLOW_RANGE), mob -> mob instanceof Enemy && mob.isAlive())) {
			mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0));
			revealed++;
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 0.6f);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.6, 0.8, 0.6, 0.05);
		player.getCooldowns().addCooldown(EventItems.ECLIPSE_SHARD.base(), EventItems.REVEAL_COOLDOWN_SECONDS * 20);
		return revealed;
	}

	@Override
	public void registerHooks() {
		Hooks.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof Vex wraith) || !EclipseWraith.is(wraith) || Creature.isSummoned(wraith)) {
				return;
			}
			ActiveEvent active = EventManager.active(level);
			if (active != null && active.event() == this) {
				EclipseState state = active.state(EclipseState::new);
				if (state.wraith == null || state.wraith.removed) {
					state.wraith = wraith;
				}
			} else {
				DeferredDiscard.discard(wraith);
				wraith.getPassengers().forEach(DeferredDiscard::discard);
			}
		});

		Hooks.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof Mob mob) || !(mob.level instanceof ServerLevel level)) {
				return;
			}
			ServerPlayer killer = source.getEntity() instanceof ServerPlayer player ? player
					: mob.getKillCredit() instanceof ServerPlayer player ? player : null;
			if (killer == null) {
				return;
			}
			if (EclipseWraith.is(mob)) {
				EclipseWraith.dropLoot(level, mob, killer, settings().getDouble("shardChance"));
				Advancements.award(killer, "defeat_eclipse_wraith");
				if (!Creature.isSummoned(mob)) {
					EventManager.score(killer, ID, 10);
				}
				for (ServerPlayer player : level.players()) {
					player.sendMessage(Lang.component(player, "mob.eclipse_wraith.defeated", killer.getName().getString()).withStyle(textColor()), net.minecraft.Util.NIL_UUID);
				}
			} else if (mob instanceof Enemy && EventManager.isActive(level, this)) {
				EventManager.score(killer, ID, 1);
			}
		});

		Hooks.USE_ITEM.register((player, world, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!EventItems.ECLIPSE_SHARD.is(stack) || !(player instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel level)
					|| player.getCooldowns().isOnCooldown(stack.getItem())) {
				return InteractionResult.PASS;
			}
			useShard(level, serverPlayer);
			return InteractionResult.SUCCESS;
		});
	}

	private static final class EclipseState {
		private final List<Mob> shaded = new ArrayList<>();
		private Vex wraith;
	}
}
