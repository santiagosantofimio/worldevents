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
import dev.santiagosantofimio.worldevents.mob.HordeWarlord;
import dev.santiagosantofimio.worldevents.mob.MobSpawning;
import dev.santiagosantofimio.worldevents.platform.Hooks;
import dev.santiagosantofimio.worldevents.util.Advancements;
import dev.santiagosantofimio.worldevents.util.DayCycle;
import dev.santiagosantofimio.worldevents.util.DeferredDiscard;
import dev.santiagosantofimio.worldevents.util.WeightedPicker;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;

/**
 * A horde: three waves of monsters come out of the dark for the players, each bigger than the last, and the Horde
 * Warlord leads the third. Clearing a wave brings the next after a short break; beating the warlord and the last
 * wave wins. Whatever is left of the horde leaves at dawn, so no monsters stay behind. Zombies in a horde never
 * break doors, and there are no creepers.
 */
public final class HordeEvent implements WorldEvent {
	public static final String ID = "horde";
	public static final String MOB_TAG = WorldEventsMod.MOD_ID + ".horde";
	public static final int WAVES = 3;
	private static final String WAVE = "wave";
	private static final String WAVE_STARTED = "waveStarted";
	private static final String TO_SPAWN = "toSpawn";
	private static final String ENDS_AT_DAWN = "endsAtDawn";
	private static final String WARLORD_SPAWNED = "warlordSpawned";
	private static final String WARLORD_DEFEATED = "warlordDefeated";
	private static final String VICTORY = "victory";

	private static final long LATEST_START = 17000;
	private static final int FIRST_WAVE_DELAY = 10;
	private static final int MIN_SPAWN_DISTANCE = 14;
	private static final int MAX_SPAWN_DISTANCE = 26;
	/** Monsters only come out of the dark: never inside a lit base. */
	private static final int MAX_SPAWN_LIGHT = 7;
	private static final int SPAWNS_PER_PLAYER_PER_SECOND = 2;
	/** If no monster finds a spot for this long, the wave goes on with those that came. */
	private static final int GIVE_UP_SPAWNING_SECONDS = 20;
	private static final double FOLLOW_RANGE = 48.0;
	/** The last few monsters of a wave glow, so players can find them. */
	private static final int GLOW_WHEN_LEFT = 3;
	/** After a restart, monsters take a moment to load: a wave is not judged cleared before. */
	private static final int RESUME_GRACE_SECONDS = 10;
	private static final double PLAYER_CRY_RANGE = 10.0;
	private static final int PLAYER_CRY_TICKS = 200;

	/** A kind of monster in the horde, from which wave on it comes, and how often. */
	private record Attacker(EntityType<? extends Mob> type, int fromWave, int weight) {
	}

	private static final List<Attacker> ATTACKERS = List.of(
			new Attacker(EntityType.ZOMBIE, 1, 5),
			new Attacker(EntityType.SKELETON, 1, 3),
			new Attacker(EntityType.SPIDER, 1, 2),
			new Attacker(EntityType.PILLAGER, 2, 2),
			new Attacker(EntityType.VINDICATOR, 2, 1),
			new Attacker(EntityType.WITCH, 3, 1));

	@Override
	public String id() {
		return ID;
	}

	@Override
	public JsonObject defaultSettings() {
		// At most once every 5 in-game days; lasts until dawn, capped at 10 minutes.
		JsonObject json = EventSettings.defaults(true, 8, 6000, 600);
		json.addProperty("mobsPerPlayer", 4);
		json.addProperty("extraMobsPerWave", 2);
		json.addProperty("maxMobs", 30);
		json.addProperty("waveBreak", 10);
		json.addProperty("waveTimeout", 150);
		json.addProperty("warlord", true);
		json.addProperty("warlordHealth", 150.0);
		json.addProperty("axeChance", 0.35);
		json.addProperty("rewardXp", 50);
		return json;
	}

	@Override
	public BossEvent.BossBarColor barColor() {
		return BossEvent.BossBarColor.RED;
	}

	@Override
	public ChatFormatting textColor() {
		return ChatFormatting.RED;
	}

	@Override
	public Holder<SoundEvent> startSound() {
		return SoundEvents.RAID_HORN;
	}

	@Override
	public String barDetail(ActiveEvent active, String language) {
		int wave = active.data().getIntOr(WAVE, 0);
		return wave > 0 ? Lang.text(language, "event.horde.wave", wave, WAVES) : null;
	}

	@Override
	public boolean isAvailable(ServerLevel level) {
		return DayCycle.hasDayCycle(level) && level.getDifficulty() != Difficulty.PEACEFUL && level.getGameRules().get(GameRules.SPAWN_MOBS);
	}

	/** Early in the night, so there is time for all three waves before dawn. */
	@Override
	public boolean canStart(ServerLevel level) {
		return level.isDarkOutside() && DayCycle.isBetween(level, DayCycle.NIGHT_START, LATEST_START);
	}

	@Override
	public int initialDuration(ServerLevel level) {
		return DayCycle.untilDawn(level, settings().durationTicks());
	}

	@Override
	public void start(ServerLevel level, ActiveEvent active) {
		active.data().putBoolean(ENDS_AT_DAWN, DayCycle.hasDayCycle(level) && level.isDarkOutside());
	}

	@Override
	public void resume(ServerLevel level, ActiveEvent active) {
		active.state(HordeState::new).graceSeconds = RESUME_GRACE_SECONDS;
	}

	@Override
	public boolean shouldEndEarly(ServerLevel level, ActiveEvent active) {
		CompoundTag data = active.data();
		return data.getBooleanOr(VICTORY, false)
				|| data.getBooleanOr(ENDS_AT_DAWN, false) && !level.isDarkOutside()
				|| level.getDifficulty() == Difficulty.PEACEFUL;
	}

	@Override
	public void tick(ServerLevel level, ActiveEvent active) {
		HordeState state = active.state(HordeState::new);
		CompoundTag data = active.data();
		EventSettings settings = settings();
		state.mobs.removeIf(mob -> mob.isRemoved() || !mob.isAlive());
		int wave = data.getIntOr(WAVE, 0);
		tickWarlord(level, active, state, wave);
		if (data.getIntOr(TO_SPAWN, 0) > 0) {
			spawnSome(level, state, data, wave);
		}

		boolean warlordDone = wave < WAVES || !settings.getBoolean("warlord") || data.getBooleanOr(WARLORD_DEFEATED, false);
		boolean cleared = wave > 0 && data.getIntOr(TO_SPAWN, 0) == 0 && state.mobs.isEmpty() && state.warlord == null && warlordDone;
		if (state.graceSeconds > 0) {
			state.graceSeconds--;
			cleared = false;
		}
		if (cleared && wave == WAVES) {
			data.putBoolean(VICTORY, true);
			return;
		}
		boolean timedOut = wave > 0 && elapsedSeconds(active) - data.getIntOr(WAVE_STARTED, 0) >= settings.getInt("waveTimeout");
		// With nobody around, the next wave waits.
		if (wave < WAVES && (wave == 0 || cleared || timedOut) && !fighters(level).isEmpty()) {
			if (cleared && !state.announcedClear) {
				state.announcedClear = true;
				for (ServerPlayer player : level.players()) {
					player.sendSystemMessage(Lang.component(player, "event.horde.wave.cleared").withStyle(textColor()), true);
				}
			}
			if (--state.secondsToNextWave <= 0) {
				startWave(level, active, wave + 1);
			}
		}
		if (data.getIntOr(TO_SPAWN, 0) == 0 && state.mobs.size() <= GLOW_WHEN_LEFT) {
			for (Mob mob : state.mobs) {
				mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
			}
		}
	}

	@Override
	public void end(ServerLevel level, ActiveEvent active) {
		HordeState state = active.state(HordeState::new);
		// Whatever is left of the horde leaves with it (a summoned warlord is not part of it).
		for (Mob mob : level.getEntities(EntityTypeTest.forClass(Mob.class),
				mob -> mob.getTags().contains(MOB_TAG) || HordeWarlord.is(mob) && !Creature.isSummoned(mob))) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
			Creature.remove(mob);
		}
		state.mobs.clear();
		state.warlord = null;
		// Fought off by nobody (no monster could reach anyone) is no victory.
		if (!active.data().getBooleanOr(VICTORY, false) || state.fighters.isEmpty()) {
			return;
		}
		int xp = settings().getInt("rewardXp");
		for (ServerPlayer player : level.players()) {
			player.sendSystemMessage(Lang.component(player, "event.horde.victory").withStyle(ChatFormatting.GOLD));
			if (state.fighters.contains(player.getUUID())) {
				Advancements.award(player, "survive_horde");
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
		HordeState state = active.state(HordeState::new);
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
		boolean last = wave == WAVES && settings.getBoolean("warlord");
		for (ServerPlayer player : level.players()) {
			EventManager.showTitle(player,
					Lang.component(player, "event.horde.wave.title", wave).withStyle(textColor()),
					Lang.component(player, last ? "event.horde.wave.final" : "event.horde.wave.subtitle"),
					SoundEvents.RAID_HORN);
		}
		WorldEventsMod.LOGGER.info("Horde wave {} in {}: {} monsters", wave, level.dimension().identifier(), total);
	}

	/** Players the horde goes after. */
	private static List<ServerPlayer> fighters(ServerLevel level) {
		List<ServerPlayer> players = new ArrayList<>();
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator()) {
				players.add(player);
			}
		}
		return players;
	}

	/** Brings a few more monsters of the current wave in, spread over the players. */
	private static void spawnSome(ServerLevel level, HordeState state, CompoundTag data, int wave) {
		List<ServerPlayer> players = fighters(level);
		if (players.isEmpty()) {
			return;
		}
		int toSpawn = data.getIntOr(TO_SPAWN, 0);
		int spawned = 0;
		for (int i = 0; i < players.size() * SPAWNS_PER_PLAYER_PER_SECOND && toSpawn > 0; i++) {
			Mob mob = spawnAttacker(level, players.get(i % players.size()), wave);
			if (mob != null) {
				// Usually already tracked: the load callback also sees new entities.
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

	private static Mob spawnAttacker(ServerLevel level, ServerPlayer target, int wave) {
		RandomSource random = level.getRandom();
		List<Attacker> choices = ATTACKERS.stream().filter(attacker -> attacker.fromWave() <= wave).toList();
		Attacker attacker = WeightedPicker.pick(choices, Attacker::weight, random.nextDouble());
		if (attacker == null) {
			return null;
		}
		EntityType<? extends Mob> type = attacker.type();
		BlockPos spot = MobSpawning.spotNear(level, type, target.blockPosition(), MIN_SPAWN_DISTANCE, MAX_SPAWN_DISTANCE, 6, 4,
				pos -> level.getBrightness(LightLayer.BLOCK, pos) <= MAX_SPAWN_LIGHT);
		if (spot == null) {
			return null;
		}
		Mob mob = type.create(level, EntitySpawnReason.EVENT);
		if (mob == null) {
			return null;
		}
		mob.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360.0f, 0.0f);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), EntitySpawnReason.EVENT, null);
		if (mob instanceof Zombie zombie) {
			zombie.setCanBreakDoors(false);
		}
		if (mob instanceof Raider raider) {
			raider.setCanJoinRaid(false);
		}
		MobSpawning.setBase(mob, Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
		mob.addTag(MOB_TAG);
		level.addFreshEntityWithPassengers(mob);
		mob.setTarget(target);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getY() + 0.5, mob.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
		return mob;
	}

	/** Brings the warlord in with the last wave. Its boss bar and war cries are its own (see HordeWarlord). */
	private void tickWarlord(ServerLevel level, ActiveEvent active, HordeState state, int wave) {
		CompoundTag data = active.data();
		if (wave == WAVES && settings().getBoolean("warlord") && !data.getBooleanOr(WARLORD_SPAWNED, false)) {
			List<ServerPlayer> players = fighters(level);
			if (!players.isEmpty()) {
				ServerPlayer target = players.get(level.getRandom().nextInt(players.size()));
				BlockPos spot = MobSpawning.spotNear(level, EntityType.VINDICATOR, target.blockPosition(), MIN_SPAWN_DISTANCE, MAX_SPAWN_DISTANCE, 6, 8);
				if (spot != null) {
					spawnWarlord(level, active, spot, Lang.language(target));
					if (state.warlord != null) {
						state.warlord.setTarget(target);
						level.playSound(null, spot, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 4.0f, 0.6f);
					}
				}
			}
		}
		if (state.warlord != null && (state.warlord.isRemoved() || !state.warlord.isAlive())) {
			state.warlord = null;
		}
	}

	/** Spawns the warlord at {@code pos} now, for tests and admins. */
	public Vindicator spawnWarlord(ServerLevel level, ActiveEvent active, BlockPos pos, String language) {
		HordeState state = active.state(HordeState::new);
		Vindicator warlord = HordeWarlord.spawn(level, pos, language, settings().getDouble("warlordHealth"));
		if (warlord != null) {
			state.warlord = warlord;
			active.data().putBoolean(WARLORD_SPAWNED, true);
		}
		return warlord;
	}

	/** The wave the current run is on (0 before the first). */
	public static int wave(ActiveEvent active) {
		return active.data().getIntOr(WAVE, 0);
	}

	/** Monsters of the current wave that are still around. */
	public static List<Mob> waveMobs(ActiveEvent active) {
		HordeState state = active.state(HordeState::new);
		state.mobs.removeIf(mob -> mob.isRemoved() || !mob.isAlive());
		return List.copyOf(state.mobs);
	}

	/**
	 * The warlord's axe: a war cry that gives the player and the players around them Strength and Speed, and
	 * weakens and slows the monsters around them.
	 */
	private static void warCry(ServerLevel level, ServerPlayer player) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.5f, 1.1f);
		AABB area = player.getBoundingBox().inflate(PLAYER_CRY_RANGE);
		for (ServerPlayer ally : level.getEntitiesOfClass(ServerPlayer.class, area, ally -> !ally.isSpectator())) {
			ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, PLAYER_CRY_TICKS, 0));
			ally.addEffect(new MobEffectInstance(MobEffects.SPEED, PLAYER_CRY_TICKS, 0));
		}
		for (Mob mob : level.getEntitiesOfClass(Mob.class, area, mob -> mob instanceof Enemy)) {
			mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, PLAYER_CRY_TICKS / 2, 0));
			mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, PLAYER_CRY_TICKS / 2, 0));
			level.sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
		}
		for (int i = 0; i < 16; i++) {
			double angle = i * Math.PI / 8;
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + Math.cos(angle) * 2.0, player.getY() + 1.0,
					player.getZ() + Math.sin(angle) * 2.0, 1, 0.0, 0.0, 0.0, 0.0);
		}
		player.sendSystemMessage(Lang.component(player, "item.warlord_axe.war_cry").withStyle(ChatFormatting.GOLD), true);
	}

	@Override
	public void registerHooks() {
		Hooks.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof Mob mob)) {
				return;
			}
			boolean warlord = HordeWarlord.is(mob);
			if (!warlord && !mob.getTags().contains(MOB_TAG) || Creature.isSummoned(mob)) {
				return;
			}
			ActiveEvent active = EventManager.active(level);
			if (active != null && active.event() == this) {
				HordeState state = active.state(HordeState::new);
				if (warlord && mob instanceof Vindicator vindicator) {
					if (state.warlord == null || state.warlord.isRemoved()) {
						state.warlord = vindicator;
					}
				} else if (!warlord && !state.mobs.contains(mob)) {
					state.mobs.add(mob);
				}
			} else {
				// The horde left without it.
				DeferredDiscard.discard(mob);
				mob.getPassengers().forEach(DeferredDiscard::discard);
			}
		});

		Hooks.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) {
				return;
			}
			boolean warlord = HordeWarlord.is(mob);
			if (!warlord && !mob.getTags().contains(MOB_TAG)) {
				return;
			}
			ServerPlayer killer = source.getEntity() instanceof ServerPlayer player ? player
					: mob.getLastHurtByPlayer() instanceof ServerPlayer player ? player : null;
			ActiveEvent active = EventManager.active(level);
			if (active != null && active.event() == this && !Creature.isSummoned(mob)) {
				if (killer != null) {
					active.state(HordeState::new).fighters.add(killer.getUUID());
					EventManager.score(killer, ID, warlord ? 10 : 1);
				}
				if (warlord) {
					active.data().putBoolean(WARLORD_DEFEATED, true);
				}
			}
			// A warlord drops its loot wherever it came from.
			if (warlord && killer != null) {
				HordeWarlord.dropLoot(level, mob, killer, settings().getDouble("axeChance"));
				Advancements.award(killer, "defeat_horde_warlord");
				for (ServerPlayer player : level.players()) {
					player.sendSystemMessage(Lang.component(player, "mob.horde_warlord.defeated", killer.getName().getString()).withStyle(ChatFormatting.GOLD));
				}
			}
		});

		// The warlord's axe: right-click for a war cry, then a cooldown shown on the axe.
		Hooks.USE_ITEM.register((player, world, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!EventItems.WARLORD_AXE.is(stack) || !(player instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel level)
					|| player.getCooldowns().isOnCooldown(stack)) {
				return InteractionResult.PASS;
			}
			warCry(level, serverPlayer);
			player.getCooldowns().addCooldown(EventItems.WAR_CRY, EventItems.WAR_CRY_COOLDOWN_SECONDS * 20);
			return InteractionResult.SUCCESS;
		});
	}

	private static final class HordeState {
		private final List<Mob> mobs = new ArrayList<>();
		private Vindicator warlord;
		private int secondsToNextWave = FIRST_WAVE_DELAY;
		private int failedSpawnSeconds;
		private boolean announcedClear;
		private int graceSeconds;
		/** Players who killed part of the horde: they get the reward. */
		private final Set<UUID> fighters = new HashSet<>();
	}
}
