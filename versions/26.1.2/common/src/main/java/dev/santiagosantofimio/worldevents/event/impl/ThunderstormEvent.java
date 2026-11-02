package dev.santiagosantofimio.worldevents.event.impl;

import dev.santiagosantofimio.worldevents.platform.Hooks;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldevents.config.EventSettings;
import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.WorldEvent;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.mob.Creature;
import dev.santiagosantofimio.worldevents.mob.MobSpawning;
import dev.santiagosantofimio.worldevents.mob.StormSpirit;
import dev.santiagosantofimio.worldevents.util.Advancements;
import dev.santiagosantofimio.worldevents.util.DeferredDiscard;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * An intense thunderstorm: rain and thunder, lightning striking all around players, creepers charged by the storm
 * and storm spirits that call lightning down on players. The storm's own lightning is visual only, so it never
 * starts fires or changes blocks. The sky clears when it ends.
 */
public final class ThunderstormEvent implements WorldEvent {
	public static final String ID = "thunderstorm";
	private static final int MIN_STRIKE_DISTANCE = 8;
	private static final int MAX_STRIKE_DISTANCE = 40;
	private static final int CLEAR_SKY_TICKS = 12000;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public JsonObject defaultSettings() {
		// At most once every 4 in-game days; lasts 4 minutes.
		JsonObject json = EventSettings.defaults(true, 10, 4800, 240);
		json.addProperty("strikeInterval", 3);
		json.addProperty("chargeCreepers", true);
		json.addProperty("spiritsPerPlayer", 1);
		json.addProperty("maxSpirits", 6);
		json.addProperty("bottledLightningChance", 0.5);
		return json;
	}

	@Override
	public BossEvent.BossBarColor barColor() {
		return BossEvent.BossBarColor.BLUE;
	}

	@Override
	public ChatFormatting textColor() {
		return ChatFormatting.AQUA;
	}

	@Override
	public Holder<SoundEvent> startSound() {
		return SoundEvents.TRIDENT_THUNDER;
	}

	/** Needs weather: the overworld, not the Nether or the End. */
	@Override
	public boolean isAvailable(ServerLevel level) {
		return level.dimensionType().hasSkyLight() && !level.dimensionType().hasFixedTime() && !level.dimensionType().hasCeiling();
	}

	@Override
	public void start(ServerLevel level, ActiveEvent active) {
		level.getServer().setWeatherParameters(0, active.totalTicks() + 200, true, true);
	}

	@Override
	public void tick(ServerLevel level, ActiveEvent active) {
		StormState state = active.state(StormState::new);
		EventSettings settings = settings();
		RandomSource random = level.getRandom();
		if (!level.isThundering()) {
			// Something cleared the sky; the storm is not over yet.
			level.getServer().setWeatherParameters(0, active.remainingTicks() + 200, true, true);
		}
		double strikeChance = 1.0 / Math.max(1, settings.getInt("strikeInterval"));
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) {
				continue;
			}
			if (random.nextDouble() < strikeChance) {
				strikeNear(level, player, settings.getBoolean("chargeCreepers") && random.nextFloat() < 0.25f);
			}
		}
		tickSpirits(level, state, settings);
	}

	@Override
	public void end(ServerLevel level, ActiveEvent active) {
		StormState state = active.state(StormState::new);
		for (Vex spirit : state.spirits) {
			if (!spirit.isRemoved()) {
				Creature.remove(spirit);
			}
		}
		state.spirits.clear();
		level.getServer().setWeatherParameters(CLEAR_SKY_TICKS, 0, false, false);
	}

	/** A visual-only bolt somewhere around the player; sometimes on a creeper, which the storm charges. */
	private static void strikeNear(ServerLevel level, ServerPlayer player, boolean onCreeper) {
		if (onCreeper) {
			List<? extends Creeper> creepers = level.getEntities(EntityTypeTest.forClass(Creeper.class),
					creeper -> !creeper.isPowered() && creeper.distanceToSqr(player) < 32 * 32);
			if (!creepers.isEmpty()) {
				Creeper creeper = creepers.get(level.getRandom().nextInt(creepers.size()));
				LightningBolt bolt = visualBolt(level, creeper.position());
				if (bolt != null) {
					creeper.thunderHit(level, bolt);
				}
				return;
			}
		}
		RandomSource random = level.getRandom();
		double angle = random.nextDouble() * Math.PI * 2;
		double distance = Mth.nextDouble(random, MIN_STRIKE_DISTANCE, MAX_STRIKE_DISTANCE);
		int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
		int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
		// Checked before the height lookup, which would load the chunk.
		if (level.isPositionEntityTicking(new BlockPos(x, player.getBlockY(), z))) {
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
			visualBolt(level, new Vec3(x + 0.5, y, z + 0.5));
		}
	}

	private static LightningBolt visualBolt(ServerLevel level, Vec3 at) {
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
		if (bolt == null) {
			return null;
		}
		bolt.snapTo(at.x, at.y, at.z);
		bolt.setVisualOnly(true);
		level.addFreshEntity(bolt);
		return bolt;
	}

	private void tickSpirits(ServerLevel level, StormState state, EventSettings settings) {
		// Their lightning is their own (see StormSpirit); the storm only brings them.
		state.spirits.removeIf(spirit -> spirit.isRemoved() || !spirit.isAlive());
		if (level.getDifficulty() == Difficulty.PEACEFUL || state.spirits.size() >= settings.getInt("maxSpirits")) {
			return;
		}
		int perPlayer = settings.getInt("spiritsPerPlayer");
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || state.spirits.size() >= settings.getInt("maxSpirits")) {
				continue;
			}
			long near = state.spirits.stream().filter(spirit -> spirit.distanceToSqr(player) < 48 * 48).count();
			if (near >= perPlayer) {
				continue;
			}
			BlockPos ground = MobSpawning.surfaceSpot(level, EntityType.VEX, player.blockPosition(), 8, 16, 6);
			if (ground != null) {
				Vex spirit = StormSpirit.spawn(level, ground.above(Mth.nextInt(level.getRandom(), 3, 6)), Lang.language(player));
				if (spirit != null) {
					// Usually already tracked: the load callback also sees new entities.
					if (!state.spirits.contains(spirit)) {
						state.spirits.add(spirit);
					}
				}
			}
		}
	}

	/** Spawns a storm spirit at {@code pos} now, for tests and admins. */
	public Vex spawnSpirit(ServerLevel level, ActiveEvent active, BlockPos pos, String language) {
		Vex spirit = StormSpirit.spawn(level, pos, language);
		if (spirit != null && !active.state(StormState::new).spirits.contains(spirit)) {
			active.state(StormState::new).spirits.add(spirit);
		}
		return spirit;
	}

	@Override
	public void registerHooks() {
		Hooks.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof Vex spirit) || !StormSpirit.is(spirit) || Creature.isSummoned(spirit)) {
				return;
			}
			ActiveEvent active = EventManager.active(level);
			if (active != null && active.event() == this) {
				List<Vex> spirits = active.state(StormState::new).spirits;
				if (!spirits.contains(spirit)) {
					spirits.add(spirit);
				}
			} else {
				DeferredDiscard.discard(spirit);
				spirit.getPassengers().forEach(DeferredDiscard::discard);
			}
		});

		Hooks.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof Vex spirit) || !StormSpirit.is(spirit) || !(spirit.level() instanceof ServerLevel level)) {
				return;
			}
			ServerPlayer killer = source.getEntity() instanceof ServerPlayer player ? player
					: spirit.getLastHurtByPlayer() instanceof ServerPlayer player ? player : null;
			if (killer != null) {
				StormSpirit.dropLoot(level, spirit, killer, settings().getDouble("bottledLightningChance"));
				Advancements.award(killer, "calm_the_storm");
				if (!Creature.isSummoned(spirit)) {
					EventManager.score(killer, ID, 5);
				}
			}
		});

		// Bottled lightning: a real bolt where the player is looking.
		Hooks.USE_ITEM.register((player, world, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!EventItems.BOTTLED_LIGHTNING.is(stack) || !(player instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel level)) {
				return InteractionResult.PASS;
			}
			HitResult hit = player.pick(64.0, 1.0f, false);
			if (hit.getType() == HitResult.Type.MISS) {
				serverPlayer.sendSystemMessage(Lang.component(serverPlayer, "item.bottled_lightning.fail").withStyle(ChatFormatting.GRAY), true);
				return InteractionResult.FAIL;
			}
			LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
			if (bolt == null) {
				return InteractionResult.FAIL;
			}
			Vec3 at = hit.getLocation();
			bolt.snapTo(at.x, at.y, at.z);
			bolt.setCause(serverPlayer);
			level.addFreshEntity(bolt);
			stack.consume(1, player);
			return InteractionResult.SUCCESS;
		});
	}

	private static final class StormState {
		private final List<Vex> spirits = new ArrayList<>();
	}
}
