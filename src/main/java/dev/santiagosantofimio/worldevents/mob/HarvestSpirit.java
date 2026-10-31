package dev.santiagosantofimio.worldevents.mob;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.item.Items;

/**
 * The harvest spirit: a small, friendly sprite of woven wheat that keeps a player company during a blessed harvest
 * and leaves golden seeds for them. Flies after its friend like an allay (the invisible mob under the model), and
 * cannot be hurt or handed items.
 */
public final class HarvestSpirit {
	public static final Creature.Kind KIND = Creature.kind("harvest_spirit", Items.ALLAY_SPAWN_EGG, HarvestSpirit::spawn);
	public static final String TAG = KIND.tag();
	public static final String NAME_KEY = KIND.nameKey();
	public static final Creature.Part BODY = Creature.register(Creature.Part.of(Items.HAY_BLOCK, "harvest_spirit", 0.9f).bright()
			.moving(Creature.Motion.FLOAT));

	private HarvestSpirit() {
	}

	/** Spawns a spirit at {@code pos} that follows {@code friend}, if there is one. */
	public static Allay spawn(ServerLevel level, BlockPos pos, String language, ServerPlayer friend) {
		Allay spirit = EntityTypes.ALLAY.create(level, EntitySpawnReason.EVENT);
		if (spirit == null) {
			return null;
		}
		spirit.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		spirit.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
		MobSpawning.setBase(spirit, Attributes.SCALE, 1.4);
		if (friend != null) {
			// The allay's own memory of the player it likes: it stays close to them.
			spirit.getBrain().setMemory(MemoryModuleType.LIKED_PLAYER, friend.getUUID());
		}
		spirit.setPermanentlyInvulnerable(true);
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
	public static UUID friend(Allay spirit) {
		return spirit.getBrain().getMemory(MemoryModuleType.LIKED_PLAYER).orElse(null);
	}

	/** Leaves a golden seed at its friend's feet, with a sparkle around the spirit. */
	public static void leaveSeed(ServerLevel level, Allay spirit, ServerPlayer friend) {
		Drops.spawn(level, friend.position().add(0.0, 0.5, 0.0), EventItems.GOLDEN_SEED.create(Lang.language(friend), 1));
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, spirit.getX(), spirit.getY() + 0.5, spirit.getZ(), 20, 0.5, 0.5, 0.5, 0.0);
		level.playSound(null, friend.getX(), friend.getY(), friend.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0f, 1.3f);
	}

	/** A little greeting when a player touches it: hearts and a chirp. */
	public static void greet(ServerLevel level, Allay spirit) {
		level.sendParticles(ParticleTypes.HEART, spirit.getX(), spirit.getY() + 0.8, spirit.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
		level.playSound(null, spirit.getX(), spirit.getY(), spirit.getZ(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.NEUTRAL, 1.0f, 1.2f);
	}

	/** Removes the spirit in a puff of leaves. */
	public static void vanish(ServerLevel level, Allay spirit) {
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, spirit.getX(), spirit.getY() + 0.5, spirit.getZ(), 15, 0.4, 0.4, 0.4, 0.0);
		Creature.remove(spirit);
	}
}
