package dev.santiagosantofimio.worldevents.event.impl;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.config.EventSettings;
import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.WorldEvent;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.mob.Creature;
import dev.santiagosantofimio.worldevents.mob.MagmaColossus;
import dev.santiagosantofimio.worldevents.mob.MobSpawning;
import dev.santiagosantofimio.worldevents.util.Advancements;
import dev.santiagosantofimio.worldevents.util.DeferredDiscard;
import dev.santiagosantofimio.worldevents.util.Drops;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * An eruption, in the Nether only: geysers of fire burst from the ground around players, with a moment's warning to
 * step aside, and the magma colossus comes up to hunt them. Monsters killed meanwhile may leave a magma heart.
 * Nothing here touches a block: geysers and the colossus's slam only hurt what stands in them.
 */
public final class EruptionEvent implements WorldEvent {
	public static final String ID = "eruption";
	private static final String COLOSSUS_SPAWNED = "colossusSpawned";
	private static final int GEYSER_WARNING_SECONDS = 2;
	private static final int MIN_GEYSER_DISTANCE = 3;
	private static final int MAX_GEYSER_DISTANCE = 10;
	private static final int GEYSER_HEIGHT = 7;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public JsonObject defaultSettings() {
		// At most once every 4 in-game days; lasts 5 minutes.
		JsonObject json = EventSettings.defaults(true, 10, 4800, 300);
		json.addProperty("geyserInterval", 6);
		json.addProperty("geyserDamage", 6.0);
		json.addProperty("colossus", true);
		json.addProperty("colossusDelay", 60);
		json.addProperty("colossusHealth", 250.0);
		json.addProperty("hammerChance", 0.35);
		json.addProperty("magmaHeartChance", 0.05);
		return json;
	}

	@Override
	public BossEvent.BossBarColor barColor() {
		return BossEvent.BossBarColor.RED;
	}

	@Override
	public ChatFormatting textColor() {
		return ChatFormatting.GOLD;
	}

	@Override
	public Holder<SoundEvent> startSound() {
		return SoundEvents.RAID_HORN;
	}

	@Override
	public boolean isAvailable(ServerLevel level) {
		return level.dimension() == Level.NETHER;
	}

	@Override
	public void tick(ServerLevel level, ActiveEvent active) {
		EruptionState state = active.state(EruptionState::new);
		EventSettings settings = settings();
		Iterator<Geyser> geysers = state.geysers.iterator();
		while (geysers.hasNext()) {
			Geyser geyser = geysers.next();
			if (--geyser.seconds <= 0) {
				geysers.remove();
				erupt(level, geyser.at, (float) settings.getDouble("geyserDamage"));
			} else {
				rumble(level, geyser.at);
			}
		}
		if (--state.secondsToGeysers <= 0) {
			state.secondsToGeysers = Math.max(1, settings.getInt("geyserInterval"));
			for (ServerPlayer player : level.players()) {
				BlockPos spot = player.isSpectator() ? null
						: MobSpawning.spotNear(level, EntityTypes.ZOMBIE, player.blockPosition(), MIN_GEYSER_DISTANCE, MAX_GEYSER_DISTANCE, 4, 6);
				if (spot != null) {
					state.geysers.add(new Geyser(spot, GEYSER_WARNING_SECONDS));
					rumble(level, spot);
				}
			}
		}
		tickColossus(level, active, state);
	}

	@Override
	public void end(ServerLevel level, ActiveEvent active) {
		EruptionState state = active.state(EruptionState::new);
		state.geysers.clear();
		if (state.colossus != null && !state.colossus.isRemoved()) {
			level.sendParticles(ParticleTypes.LAVA, state.colossus.getX(), state.colossus.getY() + 1.0, state.colossus.getZ(), 40, 1.0, 1.0, 1.0, 0.0);
			Creature.remove(state.colossus);
			for (ServerPlayer player : level.players()) {
				player.sendSystemMessage(Lang.component(player, "mob.magma_colossus.retreat").withStyle(textColor()));
			}
		}
		state.colossus = null;
	}

	/** The warning: smoke and lava pops, so players have a moment to step aside. */
	private static void rumble(ServerLevel level, BlockPos at) {
		level.sendParticles(ParticleTypes.LARGE_SMOKE, at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5, 12, 0.5, 0.05, 0.5, 0.01);
		level.sendParticles(ParticleTypes.LAVA, at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5, 6, 0.4, 0.05, 0.4, 0.0);
		level.playSound(null, at, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 1.5f, 0.6f);
	}

	/** A column of fire at {@code at}: burns and throws whatever stands in it, and leaves blocks alone. */
	public static void erupt(ServerLevel level, BlockPos at, float damage) {
		double x = at.getX() + 0.5;
		double z = at.getZ() + 0.5;
		for (int y = 0; y < GEYSER_HEIGHT; y++) {
			level.sendParticles(ParticleTypes.FLAME, x, at.getY() + y, z, 10, 0.3, 0.4, 0.3, 0.02);
		}
		level.sendParticles(ParticleTypes.LAVA, x, at.getY() + 0.5, z, 12, 0.4, 2.0, 0.4, 0.0);
		level.playSound(null, at, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.5f, 0.6f);
		AABB column = new AABB(at).inflate(1.0, 0.0, 1.0).expandTowards(0.0, GEYSER_HEIGHT, 0.0);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, column, entity -> entity.isAlive() && Creature.kindOf(entity) == null)) {
			target.hurtServer(level, level.damageSources().hotFloor(), damage);
			target.igniteForSeconds(3.0f);
			target.push(0.0, 0.9, 0.0);
			target.needsSync = true;
		}
	}

	/** Brings the colossus up once the eruption is under way. Its boss bar and slams are its own. */
	private void tickColossus(ServerLevel level, ActiveEvent active, EruptionState state) {
		EventSettings settings = settings();
		int elapsed = (active.totalTicks() - active.remainingTicks()) / 20;
		if (settings.getBoolean("colossus") && !active.data().getBooleanOr(COLOSSUS_SPAWNED, false) && elapsed >= settings.getInt("colossusDelay")
				&& level.getDifficulty() != Difficulty.PEACEFUL) {
			List<ServerPlayer> players = new ArrayList<>();
			for (ServerPlayer player : level.players()) {
				if (!player.isSpectator()) {
					players.add(player);
				}
			}
			if (!players.isEmpty()) {
				ServerPlayer target = players.get(level.getRandom().nextInt(players.size()));
				BlockPos spot = MobSpawning.spotNear(level, EntityTypes.HOGLIN, target.blockPosition(), 12, 24, 8, 10);
				if (spot != null && spawnColossus(level, active, spot, Lang.language(target)) != null) {
					level.playSound(null, spot, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 4.0f, 0.5f);
					for (ServerPlayer player : level.players()) {
						player.sendSystemMessage(Lang.component(player, "mob.magma_colossus.arrived").withStyle(textColor()));
					}
					WorldEventsMod.LOGGER.info("Magma colossus spawned at {}", spot);
				}
			}
		}
		if (state.colossus != null && (state.colossus.isRemoved() || !state.colossus.isAlive())) {
			state.colossus = null;
		}
	}

	/** Spawns the colossus at {@code pos} now, for tests and admins. */
	public Hoglin spawnColossus(ServerLevel level, ActiveEvent active, BlockPos pos, String language) {
		Hoglin colossus = MagmaColossus.spawn(level, pos, language, settings().getDouble("colossusHealth"));
		if (colossus != null) {
			active.state(EruptionState::new).colossus = colossus;
			active.data().putBoolean(COLOSSUS_SPAWNED, true);
		}
		return colossus;
	}

	@Override
	public void registerHooks() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof Hoglin colossus) || !MagmaColossus.is(colossus) || Creature.isSummoned(colossus)) {
				return;
			}
			ActiveEvent active = EventManager.active(level);
			if (active != null && active.event() == this) {
				EruptionState state = active.state(EruptionState::new);
				if (state.colossus == null || state.colossus.isRemoved()) {
					state.colossus = colossus;
				}
			} else {
				DeferredDiscard.discard(colossus);
				colossus.getPassengers().forEach(DeferredDiscard::discard);
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
			if (MagmaColossus.is(mob)) {
				MagmaColossus.dropLoot(level, mob, killer, settings().getDouble("hammerChance"));
				Advancements.award(killer, "defeat_magma_colossus");
				if (!Creature.isSummoned(mob)) {
					EventManager.score(killer, ID, 10);
				}
				for (ServerPlayer player : level.players()) {
					player.sendSystemMessage(Lang.component(player, "mob.magma_colossus.defeated", killer.getName().getString()).withStyle(textColor()));
				}
			} else if (mob instanceof Enemy && EventManager.isActive(level, this)) {
				EventManager.score(killer, ID, 1);
				if (level.getRandom().nextDouble() < settings().getDouble("magmaHeartChance")) {
					Drops.spawn(level, mob.position().add(0.0, 0.5, 0.0), EventItems.MAGMA_HEART.create(Lang.language(killer), 1));
				}
			}
		});

		// The colossus's hammer: every hit sets its target on fire.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (damageTaken > 0 && source.getDirectEntity() instanceof Player player && source.getEntity() == player
					&& EventItems.COLOSSUS_HAMMER.is(player.getMainHandItem())) {
				entity.igniteForSeconds(4.0f);
			}
		});
	}

	/** A geyser about to burst. */
	private static final class Geyser {
		private final BlockPos at;
		private int seconds;

		private Geyser(BlockPos at, int seconds) {
			this.at = at;
			this.seconds = seconds;
		}
	}

	private static final class EruptionState {
		private final List<Geyser> geysers = new ArrayList<>();
		private int secondsToGeysers = 3;
		private Hoglin colossus;
	}
}
