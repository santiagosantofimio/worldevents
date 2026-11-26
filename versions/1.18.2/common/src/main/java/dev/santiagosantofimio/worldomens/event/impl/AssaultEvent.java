package dev.santiagosantofimio.worldomens.event.impl;

import dev.santiagosantofimio.worldomens.platform.Hooks;
import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldomens.WorldOmensMod;
import dev.santiagosantofimio.worldomens.config.EventSettings;
import dev.santiagosantofimio.worldomens.event.ActiveEvent;
import dev.santiagosantofimio.worldomens.event.EventManager;
import dev.santiagosantofimio.worldomens.event.WorldEvent;
import dev.santiagosantofimio.worldomens.i18n.Lang;
import dev.santiagosantofimio.worldomens.mob.Creature;
import dev.santiagosantofimio.worldomens.mob.MobSpawning;
import dev.santiagosantofimio.worldomens.util.Advancements;
import dev.santiagosantofimio.worldomens.util.DayCycle;
import dev.santiagosantofimio.worldomens.util.DeferredDiscard;
import dev.santiagosantofimio.worldomens.util.WeightedPicker;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.util.Random;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.GameRules;

public abstract class AssaultEvent implements WorldEvent {
	public static final int WAVES = 3;
	private static final String WAVE = "wave";
	private static final String WAVE_STARTED = "waveStarted";
	private static final String TO_SPAWN = "toSpawn";
	private static final String ENDS_AT_DAWN = "endsAtDawn";
	private static final String BOSS_SPAWNED = "bossSpawned";
	private static final String BOSS_DEFEATED = "bossDefeated";
	private static final String VICTORY = "victory";

	private static final int FIRST_WAVE_DELAY = 10;
	private static final int MIN_SPAWN_DISTANCE = 14;
	private static final int MAX_SPAWN_DISTANCE = 26;
	private static final int SPAWNS_PER_PLAYER_PER_SECOND = 2;
	private static final int GIVE_UP_SPAWNING_SECONDS = 20;
	private static final double FOLLOW_RANGE = 48.0;
	private static final int GLOW_WHEN_LEFT = 3;
	private static final int RESUME_GRACE_SECONDS = 10;

	protected record Attacker(EntityType<? extends Mob> type, int fromWave, int weight) {
	}

	protected abstract String mobTag();

	protected abstract List<Attacker> attackers();

	protected abstract Creature.Kind bossKind();

	protected abstract EntityType<? extends Mob> bossType();

	protected abstract Mob spawnBoss(ServerLevel level, BlockPos pos, String language, double health);

	protected abstract void dropBossLoot(ServerLevel level, Mob boss, ServerPlayer killer, double relicChance);

	protected abstract String victoryAdvancement();

	protected abstract String bossAdvancement();

	protected abstract SoundEvent bossSound();

	protected abstract boolean startsAtNight();

	protected abstract boolean allowedHere(ServerLevel level);

	protected abstract BlockPos findSpot(ServerLevel level, EntityType<? extends Mob> type, ServerPlayer target, int minDistance, int maxDistance);

	protected void prepare(Mob mob) {
	}

	protected void engage(Mob mob, ServerPlayer target) {
		mob.setTarget(target);
	}

	protected JsonObject assaultDefaults(int weight, int cooldown, int duration) {
		JsonObject json = EventSettings.defaults(true, weight, cooldown, duration);
		json.addProperty("mobsPerPlayer", 4);
		json.addProperty("extraMobsPerWave", 2);
		json.addProperty("maxMobs", 30);
		json.addProperty("waveBreak", 10);
		json.addProperty("waveTimeout", 150);
		json.addProperty("boss", true);
		json.addProperty("bossHealth", 150.0);
		json.addProperty("relicChance", 0.35);
		json.addProperty("rewardXp", 50);
		return json;
	}

	@Override
	public String barDetail(ActiveEvent active, String language) {
		int wave = active.data().getInt(WAVE);
		return wave > 0 ? Lang.text(language, "event." + id() + ".wave", wave, WAVES) : null;
	}

	@Override
	public boolean isAvailable(ServerLevel level) {
		return allowedHere(level) && level.getDifficulty() != Difficulty.PEACEFUL && level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
	}

	@Override
	public int initialDuration(ServerLevel level) {
		return startsAtNight() ? DayCycle.untilDawn(level, settings().durationTicks()) : settings().durationTicks();
	}

	@Override
	public void start(ServerLevel level, ActiveEvent active) {
		active.data().putBoolean(ENDS_AT_DAWN, startsAtNight() && DayCycle.hasDayCycle(level) && level.isNight());
	}

	@Override
	public void resume(ServerLevel level, ActiveEvent active) {
		active.state(AssaultState::new).graceSeconds = RESUME_GRACE_SECONDS;
	}

	@Override
	public boolean shouldEndEarly(ServerLevel level, ActiveEvent active) {
		CompoundTag data = active.data();
		return data.getBoolean(VICTORY)
				|| data.getBoolean(ENDS_AT_DAWN) && !level.isNight()
				|| level.getDifficulty() == Difficulty.PEACEFUL;
	}

	@Override
	public void tick(ServerLevel level, ActiveEvent active) {
		AssaultState state = active.state(AssaultState::new);
		CompoundTag data = active.data();
		EventSettings settings = settings();
		state.mobs.removeIf(mob -> mob.isRemoved() || !mob.isAlive());
		int wave = data.getInt(WAVE);
		tickBoss(level, active, state, wave);
		if (data.getInt(TO_SPAWN) > 0) {
			spawnSome(level, state, data, wave);
		}

		boolean bossDone = wave < WAVES || !settings.getBoolean("boss") || data.getBoolean(BOSS_DEFEATED);
		boolean cleared = wave > 0 && data.getInt(TO_SPAWN) == 0 && state.mobs.isEmpty() && state.boss == null && bossDone;
		if (state.graceSeconds > 0) {
			state.graceSeconds--;
			cleared = false;
		}
		if (cleared && wave == WAVES) {
			data.putBoolean(VICTORY, true);
			return;
		}
		boolean timedOut = wave > 0 && elapsedSeconds(active) - data.getInt(WAVE_STARTED) >= settings.getInt("waveTimeout");
		if (wave < WAVES && (wave == 0 || cleared || timedOut) && !fighters(level).isEmpty()) {
			if (cleared && !state.announcedClear) {
				state.announcedClear = true;
				for (ServerPlayer player : level.players()) {
					player.displayClientMessage(Lang.component(player, "event." + id() + ".wave.cleared").withStyle(textColor()), true);
				}
			}
			if (--state.secondsToNextWave <= 0) {
				startWave(level, active, wave + 1);
			}
		}
		if (data.getInt(TO_SPAWN) == 0 && state.mobs.size() <= GLOW_WHEN_LEFT) {
			for (Mob mob : state.mobs) {
				mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
			}
		}
	}

	@Override
	public void end(ServerLevel level, ActiveEvent active) {
		AssaultState state = active.state(AssaultState::new);
		for (Mob mob : level.getEntities(EntityTypeTest.forClass(Mob.class),
				mob -> mob.getTags().contains(mobTag()) || bossKind().is(mob) && !Creature.isSummoned(mob))) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
			Creature.remove(mob);
		}
		state.mobs.clear();
		state.boss = null;
		if (!active.data().getBoolean(VICTORY) || state.fighters.isEmpty()) {
			return;
		}
		int xp = settings().getInt("rewardXp");
		for (ServerPlayer player : level.players()) {
			player.sendMessage(Lang.component(player, "event." + id() + ".victory").withStyle(ChatFormatting.GOLD), net.minecraft.Util.NIL_UUID);
			if (state.fighters.contains(player.getUUID())) {
				Advancements.award(player, victoryAdvancement());
				if (xp > 0) {
					ExperienceOrb.award(level, player.position(), xp);
				}
			}
		}
	}

	private static int elapsedSeconds(ActiveEvent active) {
		return (active.totalTicks() - active.remainingTicks()) / 20;
	}

	private void startWave(ServerLevel level, ActiveEvent active, int wave) {
		AssaultState state = active.state(AssaultState::new);
		CompoundTag data = active.data();
		EventSettings settings = settings();
		int perPlayer = settings.getInt("mobsPerPlayer") + (wave - 1) * settings.getInt("extraMobsPerWave");
		int total = Math.min(settings.getInt("maxMobs"), fighters(level).size() * perPlayer);
		data.putInt(WAVE, wave);
		data.putInt(WAVE_STARTED, elapsedSeconds(active));
		data.putInt(TO_SPAWN, Math.max(0, total));
		state.secondsToNextWave = Math.max(1, settings.getInt("waveBreak"));
		state.failedSpawnSeconds = 0;
		state.announcedClear = false;
		boolean last = wave == WAVES && settings.getBoolean("boss");
		for (ServerPlayer player : level.players()) {
			EventManager.showTitle(player,
					Lang.component(player, "event." + id() + ".wave.title", wave).withStyle(textColor()),
					Lang.component(player, last ? "event." + id() + ".wave.final" : "event." + id() + ".wave.subtitle"),
					startSound());
		}
		WorldOmensMod.LOGGER.info("{} wave {} in {}: {} attackers", id(), wave, level.dimension().location(), total);
	}

	protected static List<ServerPlayer> fighters(ServerLevel level) {
		List<ServerPlayer> players = new ArrayList<>();
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator()) {
				players.add(player);
			}
		}
		return players;
	}

	private void spawnSome(ServerLevel level, AssaultState state, CompoundTag data, int wave) {
		List<ServerPlayer> players = fighters(level);
		if (players.isEmpty()) {
			return;
		}
		int toSpawn = data.getInt(TO_SPAWN);
		int spawned = 0;
		for (int i = 0; i < players.size() * SPAWNS_PER_PLAYER_PER_SECOND && toSpawn > 0; i++) {
			Mob mob = spawnAttacker(level, players.get(i % players.size()), wave);
			if (mob != null) {
				if (!state.mobs.contains(mob)) {
					state.mobs.add(mob);
				}
				toSpawn--;
				spawned++;
			}
		}
		if (spawned > 0) {
			state.failedSpawnSeconds = 0;
		} else if (++state.failedSpawnSeconds >= GIVE_UP_SPAWNING_SECONDS) {
			toSpawn = 0;
		}
		data.putInt(TO_SPAWN, toSpawn);
	}

	private Mob spawnAttacker(ServerLevel level, ServerPlayer target, int wave) {
		Random random = level.getRandom();
		List<Attacker> choices = attackers().stream().filter(attacker -> attacker.fromWave() <= wave).toList();
		Attacker attacker = WeightedPicker.pick(choices, Attacker::weight, random.nextDouble());
		if (attacker == null) {
			return null;
		}
		EntityType<? extends Mob> type = attacker.type();
		BlockPos spot = findSpot(level, type, target, MIN_SPAWN_DISTANCE, MAX_SPAWN_DISTANCE);
		if (spot == null) {
			return null;
		}
		Mob mob = type.create(level);
		if (mob == null) {
			return null;
		}
		mob.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360.0f, 0.0f);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.EVENT, null, null);
		prepare(mob);
		MobSpawning.setBase(mob, Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
		mob.addTag(mobTag());
		level.addFreshEntityWithPassengers(mob);
		engage(mob, target);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getY() + 0.5, mob.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
		return mob;
	}

	private void tickBoss(ServerLevel level, ActiveEvent active, AssaultState state, int wave) {
		CompoundTag data = active.data();
		if (wave == WAVES && settings().getBoolean("boss") && !data.getBoolean(BOSS_SPAWNED)) {
			List<ServerPlayer> players = fighters(level);
			if (!players.isEmpty()) {
				ServerPlayer target = players.get(level.getRandom().nextInt(players.size()));
				BlockPos spot = findSpot(level, bossType(), target, MIN_SPAWN_DISTANCE, MAX_SPAWN_DISTANCE);
				if (spot != null) {
					Mob boss = spawnBoss(level, spot, Lang.language(target), settings().getDouble("bossHealth"));
					if (boss != null) {
						state.boss = boss;
						data.putBoolean(BOSS_SPAWNED, true);
						engage(boss, target);
						level.playSound(null, spot, bossSound(), SoundSource.HOSTILE, 4.0f, 0.6f);
					}
				}
			}
		}
		if (state.boss != null && (state.boss.isRemoved() || !state.boss.isAlive())) {
			state.boss = null;
		}
	}

	public static int wave(ActiveEvent active) {
		return active.data().getInt(WAVE);
	}

	public static List<Mob> waveMobs(ActiveEvent active) {
		AssaultState state = active.state(AssaultState::new);
		state.mobs.removeIf(mob -> mob.isRemoved() || !mob.isAlive());
		return List.copyOf(state.mobs);
	}

	@Override
	public void registerHooks() {
		Hooks.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof Mob mob)) {
				return;
			}
			boolean boss = bossKind().is(mob);
			if (!boss && !mob.getTags().contains(mobTag()) || Creature.isSummoned(mob)) {
				return;
			}
			ActiveEvent active = EventManager.active(level);
			if (active != null && active.event() == this) {
				AssaultState state = active.state(AssaultState::new);
				if (boss) {
					if (state.boss == null || state.boss.isRemoved()) {
						state.boss = mob;
					}
				} else if (!state.mobs.contains(mob)) {
					state.mobs.add(mob);
				}
			} else {
				DeferredDiscard.discard(mob);
				mob.getPassengers().forEach(DeferredDiscard::discard);
			}
		});

		Hooks.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof Mob mob) || !(mob.level instanceof ServerLevel level)) {
				return;
			}
			boolean boss = bossKind().is(mob);
			if (!boss && !mob.getTags().contains(mobTag())) {
				return;
			}
			ServerPlayer killer = source.getEntity() instanceof ServerPlayer player ? player
					: mob.getKillCredit() instanceof ServerPlayer player ? player : null;
			ActiveEvent active = EventManager.active(level);
			if (active != null && active.event() == this && !Creature.isSummoned(mob)) {
				if (killer != null) {
					active.state(AssaultState::new).fighters.add(killer.getUUID());
					EventManager.score(killer, id(), boss ? 10 : 1);
				}
				if (boss) {
					active.data().putBoolean(BOSS_DEFEATED, true);
				}
			}
			if (boss && killer != null) {
				dropBossLoot(level, mob, killer, settings().getDouble("relicChance"));
				Advancements.award(killer, bossAdvancement());
				for (ServerPlayer player : level.players()) {
					player.sendMessage(Lang.component(player, "mob." + bossKind().id() + ".defeated", killer.getName().getString()).withStyle(ChatFormatting.GOLD), net.minecraft.Util.NIL_UUID);
				}
			}
		});
	}

	protected static final class AssaultState {
		private final List<Mob> mobs = new ArrayList<>();
		private Mob boss;
		private int secondsToNextWave = FIRST_WAVE_DELAY;
		private int failedSpawnSeconds;
		private boolean announcedClear;
		private int graceSeconds;
		private final Set<UUID> fighters = new HashSet<>();
	}

	@Override
	public abstract SoundEvent startSound();

	@Override
	public abstract BossEvent.BossBarColor barColor();
}
