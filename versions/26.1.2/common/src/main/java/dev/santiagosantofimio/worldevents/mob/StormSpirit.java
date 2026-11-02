package dev.santiagosantofimio.worldevents.mob;

import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.util.Drops;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * The storm spirit: a small angry thundercloud that drifts through walls during a thunderstorm and calls lightning
 * down on players. Flies like a vex (the invisible mob under the model).
 */
public final class StormSpirit {
	public static final Creature.Kind KIND = Creature.kind("storm_spirit", Items.VEX_SPAWN_EGG,
					(level, pos, language, summoner) -> spawn(level, pos, language))
			.acting((level, spirit, tick) -> {
				if (Creature.everySeconds(spirit, tick, StormSpirit.STRIKE_SECONDS)) {
					strike(level, spirit);
				}
			});
	public static final String TAG = KIND.tag();
	public static final String NAME_KEY = KIND.nameKey();
	public static final Creature.Part BODY = Creature.register(Creature.Part.of(Items.WHITE_WOOL, "storm_spirit", 1.3f).bright()
			.moving(Creature.Motion.FLOAT));
	private static final int STRIKE_SECONDS = 6;
	private static final double STRIKE_RANGE = 24.0;
	private static final float STRIKE_DAMAGE = 4.0f;
	private static final int XP_REWARD = 20;

	private StormSpirit() {
	}

	public static Vex spawn(ServerLevel level, BlockPos pos, String language) {
		Vex vex = EntityType.VEX.create(level, EntitySpawnReason.EVENT);
		if (vex == null) {
			return null;
		}
		vex.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		vex.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
		// A vex spawns with a sword, which would float next to the cloud.
		vex.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		MobSpawning.setBase(vex, Attributes.SCALE, 1.6);
		MobSpawning.setBase(vex, Attributes.MAX_HEALTH, 24.0);
		vex.setHealth(vex.getMaxHealth());
		MobSpawning.name(vex, NAME_KEY, language, ChatFormatting.AQUA);
		Creature.mark(vex, KIND);
		level.addFreshEntity(vex);
		Creature.dress(vex, BODY);
		return vex;
	}

	public static boolean is(Entity entity) {
		return KIND.is(entity);
	}

	/** Calls lightning on the spirit's target if it is a player in range. Returns whether it struck. */
	public static boolean strike(ServerLevel level, Mob spirit) {
		LivingEntity target = spirit.getTarget();
		if (!(target instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()
				|| player.distanceToSqr(spirit) > STRIKE_RANGE * STRIKE_RANGE) {
			return false;
		}
		// A visual-only bolt (no fire, no block changes) plus our own damage, so storms never burn bases.
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
		if (bolt != null) {
			bolt.snapTo(player.getX(), player.getY(), player.getZ());
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}
		player.hurtServer(level, level.damageSources().lightningBolt(), STRIKE_DAMAGE);
		player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
		return true;
	}

	public static void dropLoot(ServerLevel level, Mob spirit, ServerPlayer killer, double bottleChance) {
		Vec3 at = spirit.position().add(0.0, 0.5, 0.0);
		if (level.getRandom().nextDouble() < bottleChance) {
			Drops.spawn(level, at, EventItems.BOTTLED_LIGHTNING.create(Lang.language(killer), 1));
		}
		ExperienceOrb.award(level, at, XP_REWARD);
	}
}
