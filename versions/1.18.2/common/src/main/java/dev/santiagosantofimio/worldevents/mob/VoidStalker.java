package dev.santiagosantofimio.worldevents.mob;

import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.util.Drops;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * The void stalker: a tall, thin shadow with burning eyes that blinks behind its prey every few seconds, and whose
 * touch blinds. Walks and fights like a vindicator (the invisible mob under the model), without the axe.
 */
public final class VoidStalker {
	public static final Creature.Kind KIND = Creature.kind("void_stalker", Items.ENDERMAN_SPAWN_EGG,
					(level, pos, language, summoner) -> spawn(level, pos, language))
			.acting(VoidStalker::act);
	public static final String TAG = KIND.tag();
	public static final String NAME_KEY = KIND.nameKey();
	public static final Creature.Part BODY = Creature.register(Creature.Part.of(Items.CRYING_OBSIDIAN, "void_stalker", 1.05f).big());
	private static final int BLINK_SECONDS = 6;
	private static final double MIN_BLINK_DISTANCE = 4.0;
	private static final double MAX_BLINK_DISTANCE = 32.0;
	private static final double BEHIND_TARGET = 2.5;
	private static final int XP_REWARD = 20;

	private VoidStalker() {
	}

	public static Vindicator spawn(ServerLevel level, BlockPos pos, String language) {
		Vindicator stalker = EntityType.VINDICATOR.create(level);
		if (stalker == null) {
			return null;
		}
		stalker.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		stalker.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
		// Not part of any raid that happens to be nearby.
		stalker.setCanJoinRaid(false);
		// A vindicator spawns with an axe, which would float next to the shadow.
		stalker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		MobSpawning.setBase(stalker, Attributes.MAX_HEALTH, 30.0);
		MobSpawning.setBase(stalker, Attributes.ATTACK_DAMAGE, 6.0);
		MobSpawning.setBase(stalker, Attributes.FOLLOW_RANGE, 40.0);
		stalker.setHealth(stalker.getMaxHealth());
		MobSpawning.name(stalker, NAME_KEY, language, ChatFormatting.DARK_PURPLE);
		Creature.mark(stalker, KIND);
		level.addFreshEntity(stalker);
		Creature.dress(stalker, BODY);
		level.sendParticles(ParticleTypes.PORTAL, stalker.getX(), stalker.getY() + 1.0, stalker.getZ(), 30, 0.4, 1.0, 0.4, 0.2);
		return stalker;
	}

	public static boolean is(Entity entity) {
		return KIND.is(entity);
	}

	/** Blinks behind whoever it is after, unless it is already on top of them or has lost them. */
	private static void act(ServerLevel level, Mob stalker, int tick) {
		LivingEntity target = stalker.getTarget();
		if (target == null || !Creature.everySeconds(stalker, tick, BLINK_SECONDS)) {
			return;
		}
		double distance = stalker.distanceToSqr(target);
		if (distance > MIN_BLINK_DISTANCE * MIN_BLINK_DISTANCE && distance < MAX_BLINK_DISTANCE * MAX_BLINK_DISTANCE) {
			blinkBehind(level, stalker, target);
		}
	}

	/** Teleports to solid ground behind {@code target}. Returns whether there was somewhere to stand. */
	public static boolean blinkBehind(ServerLevel level, Mob stalker, LivingEntity target) {
		Vec3 facing = target.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
		Vec3 behind = target.position().subtract(facing.scale(BEHIND_TARGET));
		BlockPos spot = MobSpawning.spotInColumn(level, EntityType.VINDICATOR, Mth.floor(behind.x), Mth.floor(behind.z),
				target.getBlockY() + 2, target.getBlockY() - 3);
		if (spot == null) {
			return false;
		}
		level.sendParticles(ParticleTypes.PORTAL, stalker.getX(), stalker.getY() + 1.0, stalker.getZ(), 30, 0.4, 1.0, 0.4, 0.2);
		stalker.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
		level.sendParticles(ParticleTypes.PORTAL, stalker.getX(), stalker.getY() + 1.0, stalker.getZ(), 30, 0.4, 1.0, 0.4, 0.2);
		level.playSound(null, stalker.getX(), stalker.getY(), stalker.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 0.7f);
		return true;
	}

	public static void dropLoot(ServerLevel level, Mob stalker, ServerPlayer killer, double pearlChance) {
		Vec3 at = stalker.position().add(0.0, 0.5, 0.0);
		if (level.getRandom().nextDouble() < pearlChance) {
			Drops.spawn(level, at, EventItems.UNSTABLE_PEARL.create(Lang.language(killer), 1));
		}
		ExperienceOrb.award(level, at, XP_REWARD);
	}
}
