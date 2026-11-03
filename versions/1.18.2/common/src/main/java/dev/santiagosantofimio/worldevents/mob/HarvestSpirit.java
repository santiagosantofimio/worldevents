package dev.santiagosantofimio.worldevents.mob;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.util.Drops;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * The harvest spirit: a small, friendly sprite of woven wheat that keeps a player company during a blessed harvest
 * and leaves golden seeds for them. It cannot be hurt or handed items.
 *
 * <p>Minecraft 1.18 has no allay, so the invisible mob under the model is a parrot without AI, which the spirit moves
 * itself: it floats next to its friend's shoulder and catches up when left behind.
 */
public final class HarvestSpirit {
	public static final Creature.Kind KIND = Creature.kind("harvest_spirit", Items.PARROT_SPAWN_EGG, HarvestSpirit::spawn)
			.acting(HarvestSpirit::follow);
	public static final String TAG = KIND.tag();
	public static final String NAME_KEY = KIND.nameKey();
	public static final Creature.Part BODY = Creature.register(Creature.Part.of(Items.HAY_BLOCK, "harvest_spirit", 0.9f).bright()
			.moving(Creature.Motion.FLOAT));
	/** Entity tag that names the spirit's friend: {@code worldevents.friend:<uuid>}. */
	private static final String FRIEND_TAG = WorldEventsMod.MOD_ID + ".friend:";
	private static final double SPEED = 0.35;
	private static final double TELEPORT_DISTANCE = 16.0;

	private HarvestSpirit() {
	}

	/** Spawns a spirit at {@code pos} that follows {@code friend}, if there is one. */
	public static Parrot spawn(ServerLevel level, BlockPos pos, String language, ServerPlayer friend) {
		Parrot spirit = EntityType.PARROT.create(level);
		if (spirit == null) {
			return null;
		}
		spirit.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		spirit.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
		spirit.setNoAi(true);
		spirit.setNoGravity(true);
		spirit.setSilent(true);
		if (friend != null) {
			spirit.addTag(FRIEND_TAG + friend.getUUID());
		}
		spirit.setInvulnerable(true);
		MobSpawning.name(spirit, NAME_KEY, language, ChatFormatting.GREEN);
		Creature.mark(spirit, KIND);
		level.addFreshEntity(spirit);
		Creature.dress(spirit, BODY);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, spirit.getX(), spirit.getY() + 0.5, spirit.getZ(), 15, 0.4, 0.4, 0.4, 0.0);
		return spirit;
	}

	public static boolean is(Entity entity) {
		return KIND.is(entity);
	}

	/** The player the spirit follows, or {@code null} if it has none. */
	public static UUID friend(Parrot spirit) {
		for (String tag : spirit.getTags()) {
			if (tag.startsWith(FRIEND_TAG)) {
				try {
					return UUID.fromString(tag.substring(FRIEND_TAG.length()));
				} catch (IllegalArgumentException e) {
					return null;
				}
			}
		}
		return null;
	}

	/** Floats next to its friend's shoulder, a little at a time; jumps to them when left far behind. */
	private static void follow(ServerLevel level, Mob mob, int tick) {
		if (!(mob instanceof Parrot spirit)) {
			return;
		}
		UUID id = friend(spirit);
		ServerPlayer friend = id != null ? level.getServer().getPlayerList().getPlayer(id) : null;
		if (friend == null || friend.level != level || friend.isSpectator()) {
			return;
		}
		float side = friend.getYRot() * Mth.DEG_TO_RAD;
		Vec3 target = friend.position().add(Mth.cos(side) * 1.2, 1.4, Mth.sin(side) * 1.2);
		Vec3 offset = target.subtract(spirit.position());
		double distance = offset.length();
		Vec3 next;
		if (distance > TELEPORT_DISTANCE) {
			next = target;
		} else if (distance > SPEED) {
			next = spirit.position().add(offset.scale(SPEED / distance));
		} else {
			next = target;
		}
		float yaw = (float) (Mth.atan2(friend.getZ() - next.z, friend.getX() - next.x) * Mth.RAD_TO_DEG) - 90.0f;
		spirit.moveTo(next.x, next.y, next.z, yaw, 0.0f);
		spirit.setYHeadRot(yaw);
	}

	/** Leaves a golden seed at its friend's feet, with a sparkle around the spirit. */
	public static void leaveSeed(ServerLevel level, Parrot spirit, ServerPlayer friend) {
		Drops.spawn(level, friend.position().add(0.0, 0.5, 0.0), EventItems.GOLDEN_SEED.create(Lang.language(friend), 1));
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, spirit.getX(), spirit.getY() + 0.5, spirit.getZ(), 20, 0.5, 0.5, 0.5, 0.0);
		level.playSound(null, friend.getX(), friend.getY(), friend.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0f, 1.3f);
	}

	/** A little greeting when a player touches it: hearts and a chime. */
	public static void greet(ServerLevel level, Parrot spirit) {
		level.sendParticles(ParticleTypes.HEART, spirit.getX(), spirit.getY() + 0.8, spirit.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
		level.playSound(null, spirit.getX(), spirit.getY(), spirit.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0f, 1.6f);
	}

	/** Removes the spirit in a puff of leaves. */
	public static void vanish(ServerLevel level, Parrot spirit) {
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, spirit.getX(), spirit.getY() + 0.5, spirit.getZ(), 15, 0.4, 0.4, 0.4, 0.0);
		Creature.remove(spirit);
	}
}
