package dev.santiagosantofimio.worldevents.mob;

import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.util.Advancements;
import dev.santiagosantofimio.worldevents.util.Drops;
import dev.santiagosantofimio.worldevents.util.Particles;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * The mimic sack: a living sack of gold that hops away from players during a gold rush. One hit and it bursts into
 * gold; if nobody catches it in time, it escapes. Moves like a rabbit (the invisible mob under the model).
 */
public final class MimicSack {
	public static final Creature.Kind KIND = Creature.kind("mimic_sack", Items.RABBIT_SPAWN_EGG,
			(level, pos, language, summoner) -> spawn(level, pos, language));
	public static final String TAG = KIND.tag();
	public static final String NAME_KEY = KIND.nameKey();
	public static final Creature.Part BODY = Creature.register(Creature.Part.of(Items.GOLD_BLOCK, "mimic_sack", 0.8f).outline(0xFFD700));
	private static final DustParticleOptions GOLD_DUST = Particles.dust(0xFFD700, 1.2f);

	private MimicSack() {
	}

	public static Rabbit spawn(ServerLevel level, BlockPos pos, String language) {
		Rabbit rabbit = EntityType.RABBIT.create(level);
		if (rabbit == null) {
			return null;
		}
		rabbit.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		rabbit.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
		rabbit.setBaby(false);
		rabbit.setSilent(true);
		rabbit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 0, false, false));
		MobSpawning.name(rabbit, NAME_KEY, language, ChatFormatting.GOLD);
		Creature.mark(rabbit, KIND);
		level.addFreshEntity(rabbit);
		Creature.dress(rabbit, BODY);
		level.sendParticles(GOLD_DUST, rabbit.getX(), rabbit.getY() + 0.5, rabbit.getZ(), 20, 0.4, 0.4, 0.4, 0.0);
		return rabbit;
	}

	public static boolean is(Entity entity) {
		return KIND.is(entity);
	}

	/** {@code player} hit the sack: it bursts into gold. */
	public static void caught(ServerLevel level, Rabbit sack, ServerPlayer player) {
		Random random = level.getRandom();
		Vec3 at = sack.position().add(0.0, 0.5, 0.0);
		Drops.spawn(level, at, EventItems.LUCKY_NUGGET.create(Lang.language(player), 1));
		Drops.spawn(level, at, new ItemStack(Items.GOLD_NUGGET, Mth.nextInt(random, 8, 16)));
		Drops.spawn(level, at, new ItemStack(Items.GOLD_INGOT, Mth.nextInt(random, 1, 3)));
		if (random.nextFloat() < 0.3f) {
			Drops.spawn(level, at, new ItemStack(Items.GOLD_INGOT, Mth.nextInt(random, 1, 2)));
		}
		level.sendParticles(GOLD_DUST, at.x, at.y, at.z, 30, 0.5, 0.5, 0.5, 0.0);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 1.0f, 1.0f);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 0.8f);
		Creature.remove(sack);
		player.sendMessage(Lang.component(player, "mob.mimic_sack.caught").withStyle(ChatFormatting.GOLD), net.minecraft.Util.NIL_UUID);
		Advancements.award(player, "catch_mimic_sack");
	}

	/** Nobody caught it in time. */
	public static void escape(ServerLevel level, Rabbit sack) {
		level.sendParticles(ParticleTypes.CLOUD, sack.getX(), sack.getY() + 0.5, sack.getZ(), 15, 0.3, 0.3, 0.3, 0.02);
		Creature.remove(sack);
	}
}
