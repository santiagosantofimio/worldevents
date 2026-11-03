package dev.santiagosantofimio.worldevents.mob;

import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.util.Drops;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * The living meteorite: a chunk of burning space rock with eyes that climbs out of meteor craters and hops after
 * players, setting them on fire. Moves like a magma cube (the invisible mob under the model, which also gives the
 * bursts of flame when it lands). Crumbles when the meteor shower ends.
 */
public final class LivingMeteorite {
	public static final Creature.Kind KIND = Creature.kind("living_meteorite", Items.MAGMA_CUBE_SPAWN_EGG,
					(level, pos, language, summoner) -> spawn(level, pos, language))
			.acting((level, meteorite, tick) -> {
				if (tick % 20 == 0) {
					smoke(level, meteorite);
				}
			});
	public static final String TAG = KIND.tag();
	public static final String NAME_KEY = KIND.nameKey();
	public static final Creature.Part BODY = Creature.register(Creature.Part.of(Items.MAGMA_BLOCK, "living_meteorite", 1.1f).bright());
	private static final int XP_REWARD = 40;

	private LivingMeteorite() {
	}

	public static MagmaCube spawn(ServerLevel level, BlockPos pos, String language) {
		MagmaCube cube = EntityType.MAGMA_CUBE.create(level);
		if (cube == null) {
			return null;
		}
		cube.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		cube.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
		// The smallest size, which never splits on death. 1.18 cannot set it directly, so through its saved data.
		CompoundTag size = cube.saveWithoutId(new CompoundTag());
		size.putInt("Size", 0);
		cube.load(size);
		MobSpawning.setBase(cube, Attributes.MAX_HEALTH, 40.0);
		MobSpawning.setBase(cube, Attributes.ATTACK_DAMAGE, 5.0);
		cube.setHealth(cube.getMaxHealth());
		MobSpawning.name(cube, NAME_KEY, language, ChatFormatting.LIGHT_PURPLE);
		Creature.mark(cube, KIND);
		level.addFreshEntity(cube);
		Creature.dress(cube, BODY);
		level.sendParticles(ParticleTypes.LAVA, cube.getX(), cube.getY() + 1.0, cube.getZ(), 15, 0.4, 0.6, 0.4, 0.0);
		return cube;
	}

	public static boolean is(Entity entity) {
		return KIND.is(entity);
	}

	public static void dropLoot(ServerLevel level, MagmaCube meteorite, ServerPlayer killer, int fragments, double pickaxeChance) {
		Random random = level.getRandom();
		Vec3 at = meteorite.position().add(0.0, 0.5, 0.0);
		String language = Lang.language(killer);
		Drops.spawn(level, at, EventItems.METEORITE_FRAGMENT.create(language, Math.max(1, fragments + Mth.nextInt(random, -1, 1))));
		if (random.nextDouble() < pickaxeChance) {
			Drops.spawn(level, at, EventItems.STARFALL_PICKAXE.create(language, 1));
		}
		ExperienceOrb.award(level, at, XP_REWARD);
	}

	/** Ambient effect: embers and smoke rising from the rock. */
	public static void smoke(ServerLevel level, Mob meteorite) {
		double top = meteorite.getY() + meteorite.getBbHeight();
		level.sendParticles(ParticleTypes.FLAME, meteorite.getX(), top, meteorite.getZ(), 2, 0.3, 0.1, 0.3, 0.01);
		level.sendParticles(ParticleTypes.SMOKE, meteorite.getX(), top + 0.1, meteorite.getZ(), 2, 0.3, 0.1, 0.3, 0.01);
	}
}
