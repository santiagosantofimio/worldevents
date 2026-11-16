package dev.santiagosantofimio.worldevents.mob;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The blood eye: a giant floating eyeball that drifts through walls and charges at players during a blood moon.
 * Flies like a vex (the invisible mob under the model).
 */
public final class BloodEye {
	public static final Creature.Kind KIND = Creature.kind("blood_eye", Items.VEX_SPAWN_EGG,
			(level, pos, language, summoner) -> spawn(level, pos, language));
	public static final String TAG = KIND.tag();
	public static final String NAME_KEY = KIND.nameKey();
	public static final Creature.Part BODY = Creature.register(Creature.Part.of(Items.OBSERVER, "blood_eye", 1.0f).bright());

	private BloodEye() {
	}

	public static Vex spawn(ServerLevel level, BlockPos pos, String language) {
		Vex vex = EntityTypes.VEX.create(level, EntitySpawnReason.EVENT);
		if (vex == null) {
			return null;
		}
		vex.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		vex.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
		// A vex spawns with a sword, which would float next to the eye.
		vex.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		MobSpawning.setBase(vex, Attributes.SCALE, 1.4);
		MobSpawning.setBase(vex, Attributes.MAX_HEALTH, 20.0);
		vex.setHealth(vex.getMaxHealth());
		MobSpawning.name(vex, NAME_KEY, language, ChatFormatting.RED);
		Creature.mark(vex, KIND);
		level.addFreshEntity(vex);
		Creature.dress(vex, BODY);
		return vex;
	}

	public static boolean is(Entity entity) {
		return KIND.is(entity);
	}
}
