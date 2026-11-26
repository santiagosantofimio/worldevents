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
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class EclipseEvent implements WorldEvent {
	public static final String ID = "eclipse";
	public static final String SHADED_TAG = WorldOmensMod.MOD_ID + ".shaded";
	private static final String WRAITH_SPAWNED = "wraithSpawned";
	private static final long EARLIEST_START = 1000;
	private static final long LATEST_START = 8000;
	private static final List<EntityType<? extends Mob>> MONSTERS = List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.SPIDER);
	private static final int GLOW_RANGE = 32;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public JsonObject defaultSettings() {
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
	public Holder<SoundEvent> startSound() {
		return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ELDER_GUARDIAN_CURSE);
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
		state.shaded.removeIf(mob -> mob.isRemoved() || !mob.isAlive());
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) {
				continue;
			}
			level.sendParticles(player, ParticleTypes.ASH, false, false, player.getX(), player.getY() + 2.0, player.getZ(), 30, 8.0, 3.0, 8.0, 0.0);
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
		for (Mob mob : state.shaded) {
			if (!mob.isRemoved()) {
				mob.removeEffect(MobEffects.FIRE_RESISTANCE);
				mob.removeTag(SHADED_TAG);
			}
		}
		state.shaded.clear();
		if (state.wraith != null && !state.wraith.isRemoved()) {
			level.sendParticles(ParticleTypes.SQUID_INK, state.wraith.getX(), state.wraith.getY(), state.wraith.getZ(), 60, 2.0, 1.0, 2.0, 0.05);
			Creature.remove(state.wraith);
			for (ServerPlayer player : level.players()) {
				player.sendSystemMessage(Lang.component(player, "mob.eclipse_wraith.retreat").withStyle(textColor()));
			}
		}
		state.wraith = null;
	}

	public static void shadeFromSun(Mob mob) {
		mob.addTag(SHADED_TAG);
		mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
	}

	public static void keepShaded(Mob mob) {
		if (mob.isOnFire()) {
			mob.clearFire();
			mob.setSharedFlagOnFire(false);
		}
	}

	public static Mob spawnMonster(ServerLevel level, BlockPos center) {
		EntityType<? extends Mob> type = MONSTERS.get(level.getRandom().nextInt(MONSTERS.size()));
		BlockPos spot = MobSpawning.spotNear(level, type, center, 10, 24, 6, 10);
		if (spot == null) {
			return null;
		}
		Mob mob = type.create(level, EntitySpawnReason.EVENT);
		if (mob == null) {
			return null;
		}
		mob.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), EntitySpawnReason.EVENT, null);
		shadeFromSun(mob);
		level.addFreshEntity(mob);
		level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + 1.0, mob.getZ(), 15, 0.3, 0.6, 0.3, 0.02);
		return mob;
	}

	public static Mob spawnPhantom(ServerLevel level, BlockPos center) {
		BlockPos at = center.offset(level.getRandom().nextInt(17) - 8, 18 + level.getRandom().nextInt(6), level.getRandom().nextInt(17) - 8);
		if (!level.isPositionEntityTicking(at) || !level.getBlockState(at).isAir()) {
			return null;
		}
		Phantom phantom = EntityTypes.PHANTOM.create(level, EntitySpawnReason.EVENT);
		if (phantom == null) {
			return null;
		}
		phantom.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		phantom.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.EVENT, null);
		shadeFromSun(phantom);
		level.addFreshEntity(phantom);
		return phantom;
	}

	private void tickWraith(ServerLevel level, ActiveEvent active, EclipseState state, int elapsed) {
		EventSettings settings = settings();
		if (settings.getBoolean("wraith") && !active.data().getBooleanOr(WRAITH_SPAWNED, false) && elapsed >= settings.getInt("wraithDelay")) {
			List<ServerPlayer> players = new ArrayList<>();
			for (ServerPlayer player : level.players()) {
				if (!player.isSpectator() && !player.isCreative()) {
					players.add(player);
				}
			}
			if (!players.isEmpty()) {
				ServerPlayer target = players.get(level.getRandom().nextInt(players.size()));
				BlockPos at = target.blockPosition().above(16);
				if (level.isPositionEntityTicking(at) && spawnWraith(level, active, at, Lang.language(target)) != null) {
					level.playSound(null, at, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5f, 0.6f);
					for (ServerPlayer player : level.players()) {
						player.sendSystemMessage(Lang.component(player, "mob.eclipse_wraith.arrived").withStyle(textColor()));
					}
				}
			}
		}
		if (state.wraith != null && (state.wraith.isRemoved() || !state.wraith.isAlive())) {
			state.wraith = null;
		}
	}

	public Vex spawnWraith(ServerLevel level, ActiveEvent active, BlockPos pos, String language) {
		Vex wraith = EclipseWraith.spawn(level, pos, language, settings().getDouble("wraithHealth"));
		if (wraith != null) {
			active.state(EclipseState::new).wraith = wraith;
			active.data().putBoolean(WRAITH_SPAWNED, true);
		}
		return wraith;
	}

	public static int useShard(ServerLevel level, ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0));
		int revealed = 0;
		for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(GLOW_RANGE), mob -> mob instanceof Enemy && mob.isAlive())) {
			mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0));
			revealed++;
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.6f);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.6, 0.8, 0.6, 0.05);
		player.getCooldowns().addCooldown(EventItems.REVEAL, EventItems.REVEAL_COOLDOWN_SECONDS * 20);
		return revealed;
	}

	@Override
	public void registerHooks() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof Vex wraith) || !EclipseWraith.is(wraith) || Creature.isSummoned(wraith)) {
				return;
			}
			ActiveEvent active = EventManager.active(level);
			if (active != null && active.event() == this) {
				EclipseState state = active.state(EclipseState::new);
				if (state.wraith == null || state.wraith.isRemoved()) {
					state.wraith = wraith;
				}
			} else {
				DeferredDiscard.discard(wraith);
				wraith.getPassengers().forEach(DeferredDiscard::discard);
			}
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) {
				return;
			}
			ServerPlayer killer = source.getEntity() instanceof ServerPlayer player ? player
					: mob.getLastHurtByPlayer() instanceof ServerPlayer player ? player : null;
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
					player.sendSystemMessage(Lang.component(player, "mob.eclipse_wraith.defeated", killer.getName().getString()).withStyle(textColor()));
				}
			} else if (mob instanceof Enemy && EventManager.isActive(level, this)) {
				EventManager.score(killer, ID, 1);
			}
		});

		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!EventItems.ECLIPSE_SHARD.is(stack) || !(player instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel level)
					|| player.getCooldowns().isOnCooldown(stack)) {
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
