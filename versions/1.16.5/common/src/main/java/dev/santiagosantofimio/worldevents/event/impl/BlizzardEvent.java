package dev.santiagosantofimio.worldevents.event.impl;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldevents.config.EventSettings;
import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.WorldEvent;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.mob.Creature;
import dev.santiagosantofimio.worldevents.mob.FrostYeti;
import dev.santiagosantofimio.worldevents.mob.MobSpawning;
import dev.santiagosantofimio.worldevents.util.Advancements;
import dev.santiagosantofimio.worldevents.util.DeferredDiscard;
import dev.santiagosantofimio.worldevents.platform.Hooks;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A blizzard, while someone is in a snowy biome: snow and wind, strays out in the storm, and the frost yeti. Players
 * out in the open in the cold freeze little by little, as in powder snow, unless they shelter under a roof, stand by a
 * fire or wear leather. No block is touched.
 */
public final class BlizzardEvent implements WorldEvent {
	public static final String ID = "blizzard";
	private static final String YETI_SPAWNED = "yetiSpawned";
	private static final int CLEAR_SKY_TICKS = 6000;
	private static final int HEAT_RADIUS = 4;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public JsonObject defaultSettings() {
		// At most once every 4 in-game days; lasts 5 minutes.
		JsonObject json = EventSettings.defaults(true, 8, 4800, 300);
		json.addProperty("freezing", true);
		json.addProperty("strayInterval", 15);
		json.addProperty("straysPerPlayer", 2);
		json.addProperty("yeti", true);
		json.addProperty("yetiDelay", 60);
		json.addProperty("yetiHealth", 180.0);
		json.addProperty("fangChance", 0.35);
		return json;
	}

	@Override
	public BossEvent.BossBarColor barColor() {
		return BossEvent.BossBarColor.WHITE;
	}

	@Override
	public ChatFormatting textColor() {
		return ChatFormatting.AQUA;
	}

	@Override
	public SoundEvent startSound() {
		return SoundEvents.ELYTRA_FLYING;
	}

	@Override
	public boolean isAvailable(ServerLevel level) {
		return level.dimension() == Level.OVERWORLD && level.getDifficulty() != Difficulty.PEACEFUL;
	}

	/** Only when someone is somewhere cold enough to snow: elsewhere it would just rain. */
	@Override
	public boolean canStart(ServerLevel level) {
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && isCold(level, player.blockPosition())) {
				return true;
			}
		}
		return false;
	}

	public static boolean isCold(ServerLevel level, BlockPos pos) {
		return level.getBiome(pos).getTemperature(pos) < 0.15f;
	}

	@Override
	public void start(ServerLevel level, ActiveEvent active) {
		level.setWeatherParameters(0, active.totalTicks() + 200, true, false);
	}

	@Override
	public void resume(ServerLevel level, ActiveEvent active) {
		level.setWeatherParameters(0, active.remainingTicks() + 200, true, false);
	}

	@Override
	public void tick(ServerLevel level, ActiveEvent active) {
		EventSettings settings = settings();
		BlizzardState state = active.state(BlizzardState::new);
		int elapsed = (active.totalTicks() - active.remainingTicks()) / 20;
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) {
				continue;
			}
			level.sendParticles(player, ParticleTypes.ITEM_SNOWBALL, false, player.getX(), player.getY() + 3.0, player.getZ(), 60, 8.0, 3.0, 8.0, 0.05);
			if (player.isCreative()) {
				continue;
			}
			if (settings.getBoolean("freezing") && isExposed(level, player)) {
				// 1.16 has no freezing: the cold slows the player and bites now and then.
				chill(player, 60);
				if (elapsed % 2 == 0) {
					player.hurt(DamageSource.GENERIC, 1.0f);
				}
			}
			if (elapsed > 0 && elapsed % Math.max(1, settings.getInt("strayInterval")) == 0) {
				for (int i = 0; i < settings.getInt("straysPerPlayer"); i++) {
					spawnStray(level, player.blockPosition());
				}
			}
		}
		tickYeti(level, active, state, elapsed);
	}

	@Override
	public void end(ServerLevel level, ActiveEvent active) {
		BlizzardState state = active.state(BlizzardState::new);
		level.setWeatherParameters(CLEAR_SKY_TICKS, 0, false, false);
		if (state.yeti != null && !state.yeti.removed) {
			level.sendParticles(ParticleTypes.ITEM_SNOWBALL, state.yeti.getX(), state.yeti.getY() + 1.0, state.yeti.getZ(), 80, 1.0, 1.0, 1.0, 0.05);
			Creature.remove(state.yeti);
			for (ServerPlayer player : level.players()) {
				player.sendMessage(Lang.component(player, "mob.frost_yeti.retreat").withStyle(textColor()), net.minecraft.Util.NIL_UUID);
			}
		}
		state.yeti = null;
	}

	/** Whether {@code player} is out in the cold: in a snowy biome, under the open sky, away from fire, not in leather. */
	public static boolean isExposed(ServerLevel level, Player player) {
		BlockPos pos = player.blockPosition();
		return !wearsLeather(player) && isCold(level, pos) && level.canSeeSky(pos.above()) && !nearHeat(level, pos);
	}

	private static boolean nearHeat(ServerLevel level, BlockPos center) {
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-HEAT_RADIUS, -2, -HEAT_RADIUS), center.offset(HEAT_RADIUS, 2, HEAT_RADIUS))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(BlockTags.CAMPFIRES) || state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK) || state.getFluidState().is(FluidTags.LAVA)) {
				return true;
			}
		}
		return false;
	}

	/** Whether {@code entity} wears any leather, which keeps the cold out. */
	public static boolean wearsLeather(LivingEntity entity) {
		for (ItemStack stack : entity.getArmorSlots()) {
			Item item = stack.getItem();
			if (item == Items.LEATHER_HELMET || item == Items.LEATHER_CHESTPLATE || item == Items.LEATHER_LEGGINGS || item == Items.LEATHER_BOOTS) {
				return true;
			}
		}
		return false;
	}

	/** Chills {@code entity}: slowness for {@code ticks}. Minecraft 1.16 has no freezing, so this is all the cold does. */
	public static void chill(Entity entity, int ticks) {
		if (entity instanceof LivingEntity living && !wearsLeather(living)) {
			living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 0));
		}
	}

	/** A stray out of the storm near {@code center}. */
	public static Mob spawnStray(ServerLevel level, BlockPos center) {
		BlockPos spot = MobSpawning.spotNear(level, EntityType.STRAY, center, 10, 24, 6, 10);
		if (spot == null) {
			return null;
		}
		Stray stray = EntityType.STRAY.create(level);
		if (stray == null) {
			return null;
		}
		stray.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		stray.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.EVENT, null, null);
		level.addFreshEntity(stray);
		level.sendParticles(ParticleTypes.ITEM_SNOWBALL, stray.getX(), stray.getY() + 1.0, stray.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
		return stray;
	}

	private void tickYeti(ServerLevel level, ActiveEvent active, BlizzardState state, int elapsed) {
		EventSettings settings = settings();
		if (settings.getBoolean("yeti") && !active.data().getBoolean(YETI_SPAWNED) && elapsed >= settings.getInt("yetiDelay")) {
			List<ServerPlayer> players = new ArrayList<>();
			for (ServerPlayer player : level.players()) {
				if (!player.isSpectator() && !player.isCreative()) {
					players.add(player);
				}
			}
			if (!players.isEmpty()) {
				ServerPlayer target = players.get(level.getRandom().nextInt(players.size()));
				BlockPos spot = MobSpawning.spotNear(level, EntityType.RAVAGER, target.blockPosition(), 12, 24, 8, 10);
				if (spot != null && spawnYeti(level, active, spot, Lang.language(target)) != null) {
					level.playSound(null, spot, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 4.0f, 0.6f);
					for (ServerPlayer player : level.players()) {
						player.sendMessage(Lang.component(player, "mob.frost_yeti.arrived").withStyle(textColor()), net.minecraft.Util.NIL_UUID);
					}
				}
			}
		}
		if (state.yeti != null && (state.yeti.removed || !state.yeti.isAlive())) {
			state.yeti = null;
		}
	}

	/** Spawns the yeti at {@code pos} now, for tests and admins. */
	public Ravager spawnYeti(ServerLevel level, ActiveEvent active, BlockPos pos, String language) {
		Ravager yeti = FrostYeti.spawn(level, pos, language, settings().getDouble("yetiHealth"));
		if (yeti != null) {
			active.state(BlizzardState::new).yeti = yeti;
			active.data().putBoolean(YETI_SPAWNED, true);
		}
		return yeti;
	}

	@Override
	public void registerHooks() {
		Hooks.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof Ravager yeti) || !FrostYeti.is(yeti) || Creature.isSummoned(yeti)) {
				return;
			}
			ActiveEvent active = EventManager.active(level);
			if (active != null && active.event() == this) {
				BlizzardState state = active.state(BlizzardState::new);
				if (state.yeti == null || state.yeti.removed) {
					state.yeti = yeti;
				}
			} else {
				DeferredDiscard.discard(yeti);
				yeti.getPassengers().forEach(DeferredDiscard::discard);
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
			if (FrostYeti.is(mob)) {
				FrostYeti.dropLoot(level, mob, killer, settings().getDouble("fangChance"));
				Advancements.award(killer, "defeat_frost_yeti");
				if (!Creature.isSummoned(mob)) {
					EventManager.score(killer, ID, 10);
				}
				for (ServerPlayer player : level.players()) {
					player.sendMessage(Lang.component(player, "mob.frost_yeti.defeated", killer.getName().getString()).withStyle(textColor()), net.minecraft.Util.NIL_UUID);
				}
			} else if (mob instanceof Enemy && EventManager.isActive(level, this)) {
				EventManager.score(killer, ID, 1);
			}
		});

		// Frostfang: every hit chills and slows its target.
		Hooks.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (damageTaken > 0 && source.getDirectEntity() instanceof Player player && source.getEntity() == player
					&& EventItems.FROSTFANG.is(player.getMainHandItem())) {
				frostbite(entity);
			}
		});
	}

	/** What a Frostfang hit does to its target. */
	public static void frostbite(LivingEntity target) {
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
		if (target.level instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.ITEM_SNOWBALL, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
		}
	}

	private static final class BlizzardState {
		private Ravager yeti;
	}
}
