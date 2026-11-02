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
import dev.santiagosantofimio.worldevents.mob.VoidStalker;
import dev.santiagosantofimio.worldevents.util.Advancements;
import dev.santiagosantofimio.worldevents.util.DeferredDiscard;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A void storm, in the End only: void stalkers hunt the players, and pulses of the void lift them off the ground
 * for a moment and let them float back down. The pulses only ever push straight up, so nobody is thrown off an
 * island. Stalkers may drop unstable pearls.
 */
public final class VoidStormEvent implements WorldEvent {
	public static final String ID = "void_storm";
	private static final int PULSE_LIFT_TICKS = 30;
	private static final int PULSE_FALL_TICKS = 100;
	private static final double BLINK_RANGE = 12.0;
	private static final int BLINK_FALL_TICKS = 60;
	private static final int BLINK_COOLDOWN_TICKS = 20;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public JsonObject defaultSettings() {
		// At most once every 4 in-game days; lasts 5 minutes.
		JsonObject json = EventSettings.defaults(true, 10, 4800, 300);
		json.addProperty("stalkersPerPlayer", 2);
		json.addProperty("maxStalkers", 8);
		json.addProperty("pulseInterval", 12);
		json.addProperty("pearlChance", 0.35);
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
		return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ENDERMAN_STARE);
	}

	/** Stalkers are monsters, and monsters do not exist on peaceful. */
	@Override
	public boolean isAvailable(ServerLevel level) {
		return level.dimension() == Level.END && level.getDifficulty() != Difficulty.PEACEFUL;
	}

	@Override
	public void tick(ServerLevel level, ActiveEvent active) {
		StormState state = active.state(StormState::new);
		EventSettings settings = settings();
		boolean pulse = --state.secondsToPulse <= 0;
		if (pulse) {
			state.secondsToPulse = Math.max(1, settings.getInt("pulseInterval"));
		}
		for (ServerPlayer player : level.players()) {
			level.sendParticles(player, ParticleTypes.REVERSE_PORTAL, false, false, player.getX(), player.getY() + 1.0, player.getZ(), 20, 6.0, 3.0, 6.0, 0.0);
			if (pulse && !player.isSpectator()) {
				pulse(level, player);
			}
		}
		tickStalkers(level, state, settings);
	}

	@Override
	public void end(ServerLevel level, ActiveEvent active) {
		StormState state = active.state(StormState::new);
		for (Vindicator stalker : state.stalkers) {
			if (!stalker.isRemoved()) {
				level.sendParticles(ParticleTypes.PORTAL, stalker.getX(), stalker.getY() + 1.0, stalker.getZ(), 30, 0.4, 1.0, 0.4, 0.2);
				Creature.remove(stalker);
			}
		}
		state.stalkers.clear();
	}

	/** Lifts the player straight up for a moment, and lets them float back down. */
	private static void pulse(ServerLevel level, ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, PULSE_LIFT_TICKS, 0, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, PULSE_FALL_TICKS, 0, false, false));
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 0.2, player.getZ(), 40, 1.5, 0.1, 1.5, 0.05);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_AMBIENT, SoundSource.AMBIENT, 1.0f, 0.5f);
	}

	private void tickStalkers(ServerLevel level, StormState state, EventSettings settings) {
		state.stalkers.removeIf(stalker -> stalker.isRemoved() || !stalker.isAlive());
		int max = settings.getInt("maxStalkers");
		int perPlayer = settings.getInt("stalkersPerPlayer");
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || state.stalkers.size() >= max) {
				continue;
			}
			long near = state.stalkers.stream().filter(stalker -> stalker.distanceToSqr(player) < 48 * 48).count();
			if (near < perPlayer) {
				BlockPos spot = MobSpawning.spotNear(level, EntityType.VINDICATOR, player.blockPosition(), 12, 20, 6, 6);
				if (spot != null) {
					track(state, VoidStalker.spawn(level, spot, Lang.language(player)));
				}
			}
		}
	}

	private static void track(StormState state, Vindicator stalker) {
		// Usually already tracked: the load callback also sees new entities.
		if (stalker != null && !state.stalkers.contains(stalker)) {
			state.stalkers.add(stalker);
		}
	}

	/** Spawns a stalker at {@code pos} now, for tests and admins. */
	public Vindicator spawnStalker(ServerLevel level, ActiveEvent active, BlockPos pos, String language) {
		Vindicator stalker = VoidStalker.spawn(level, pos, language);
		track(active.state(StormState::new), stalker);
		return stalker;
	}

	/**
	 * The unstable pearl: moves the player forward in a blink, stopping short of whatever is in the way. Aimed at the
	 * void it still goes, which is why they float for a few seconds afterwards. Returns whether they moved.
	 */
	public static boolean blink(ServerLevel level, ServerPlayer player) {
		Vec3 start = player.position();
		Vec3 look = player.getLookAngle();
		HitResult hit = player.pick(BLINK_RANGE, 1.0f, false);
		double reach = hit.getType() == HitResult.Type.MISS ? BLINK_RANGE : hit.getLocation().distanceTo(player.getEyePosition()) - 1.0;
		for (double distance = reach; distance >= 2.0; distance -= 0.5) {
			Vec3 to = start.add(look.scale(distance));
			AABB box = player.getBoundingBox().move(to.subtract(start));
			if (level.isLoaded(BlockPos.containing(to)) && level.noCollision(player, box)) {
				level.sendParticles(ParticleTypes.PORTAL, start.x, start.y + 1.0, start.z, 30, 0.3, 0.8, 0.3, 0.2);
				player.teleportTo(to.x, to.y, to.z);
				player.resetFallDistance();
				player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, BLINK_FALL_TICKS, 0, false, false));
				level.sendParticles(ParticleTypes.PORTAL, to.x, to.y + 1.0, to.z, 30, 0.3, 0.8, 0.3, 0.2);
				level.playSound(null, to.x, to.y, to.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.2f);
				return true;
			}
		}
		return false;
	}

	@Override
	public void registerHooks() {
		Hooks.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof Vindicator stalker) || !VoidStalker.is(stalker) || Creature.isSummoned(stalker)) {
				return;
			}
			ActiveEvent active = EventManager.active(level);
			if (active != null && active.event() == this) {
				track(active.state(StormState::new), stalker);
			} else {
				DeferredDiscard.discard(stalker);
				stalker.getPassengers().forEach(DeferredDiscard::discard);
			}
		});

		Hooks.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof Mob stalker) || !VoidStalker.is(stalker) || !(stalker.level() instanceof ServerLevel level)) {
				return;
			}
			ServerPlayer killer = source.getEntity() instanceof ServerPlayer player ? player
					: stalker.getLastHurtByPlayer() instanceof ServerPlayer player ? player : null;
			if (killer != null) {
				VoidStalker.dropLoot(level, stalker, killer, settings().getDouble("pearlChance"));
				Advancements.award(killer, "defeat_void_stalker");
				if (!Creature.isSummoned(stalker)) {
					EventManager.score(killer, ID, 3);
				}
			}
		});

		// Its touch blinds for a moment.
		Hooks.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (damageTaken > 0 && source.getEntity() instanceof Vindicator stalker && VoidStalker.is(stalker) && source.getDirectEntity() == stalker) {
				entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
			}
		});

		Hooks.USE_ITEM.register((player, world, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!EventItems.UNSTABLE_PEARL.is(stack) || !(player instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel level)
					|| player.getCooldowns().isOnCooldown(stack)) {
				return InteractionResult.PASS;
			}
			if (!blink(level, serverPlayer)) {
				serverPlayer.sendSystemMessage(Lang.component(serverPlayer, "item.unstable_pearl.fail").withStyle(ChatFormatting.GRAY), true);
				return InteractionResult.FAIL;
			}
			player.getCooldowns().addCooldown(stack, BLINK_COOLDOWN_TICKS);
			stack.consume(1, player);
			return InteractionResult.SUCCESS;
		});
	}

	private static final class StormState {
		private final List<Vindicator> stalkers = new ArrayList<>();
		private int secondsToPulse = 6;
	}
}
