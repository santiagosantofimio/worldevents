package dev.santiagosantofimio.worldevents.mob;

import dev.santiagosantofimio.worldevents.config.ModConfig;
import dev.santiagosantofimio.worldevents.event.impl.HordeEvent;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * The Horde Warlord: a hulking brute in dark horned armour and a tattered war cape who leads the last wave of a
 * horde, swinging the warlord's axe. Its war cry spurs on the monsters around it. It has its own boss bar. Moves
 * and fights like a vindicator (the invisible mob under the model, whose axe stays visible).
 */
public final class HordeWarlord {
	public static final Creature.Kind KIND = Creature.kind("horde_warlord", Items.VINDICATOR_SPAWN_EGG,
					(level, pos, language, summoner) -> spawn(level, pos, language, configuredHealth()))
			.acting(HordeWarlord::act)
			.boss(BossEvent.BossBarColor.WHITE, ChatFormatting.GOLD);
	public static final String TAG = KIND.tag();
	public static final String NAME_KEY = KIND.nameKey();
	public static final Creature.Part BODY = Creature.register(Creature.Part.of(Items.CHISELED_POLISHED_BLACKSTONE, "horde_warlord", 1.45f).big());
	private static final int WAR_CRY_SECONDS = 15;
	private static final double WAR_CRY_RANGE = 16.0;
	private static final int WAR_CRY_TICKS = 160;
	private static final int XP_REWARD = 120;

	private HordeWarlord() {
	}

	public static Vindicator spawn(ServerLevel level, BlockPos pos, String language, double health) {
		Vindicator warlord = EntityTypes.VINDICATOR.create(level, EntitySpawnReason.EVENT);
		if (warlord == null) {
			return null;
		}
		warlord.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		warlord.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
		// Not part of any raid that happens to be nearby.
		warlord.setCanJoinRaid(false);
		MobSpawning.setBase(warlord, Attributes.SCALE, 1.4);
		MobSpawning.setBase(warlord, Attributes.MAX_HEALTH, health);
		// The axe it holds adds +9, for 11 damage per hit.
		MobSpawning.setBase(warlord, Attributes.ATTACK_DAMAGE, 2.0);
		MobSpawning.setBase(warlord, Attributes.ARMOR, 8.0);
		MobSpawning.setBase(warlord, Attributes.KNOCKBACK_RESISTANCE, 0.9);
		MobSpawning.setBase(warlord, Attributes.FOLLOW_RANGE, 40.0);
		warlord.setHealth(warlord.getMaxHealth());
		warlord.setItemSlot(EquipmentSlot.MAINHAND, EventItems.WARLORD_AXE.create(language, 1));
		warlord.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
		MobSpawning.name(warlord, NAME_KEY, language, ChatFormatting.GOLD);
		Creature.mark(warlord, KIND);
		level.addFreshEntityWithPassengers(warlord);
		Creature.dress(warlord, BODY);
		return warlord;
	}

	public static boolean is(Entity entity) {
		return KIND.is(entity);
	}

	/** The health set in the horde's config, for summoned warlords too. */
	private static double configuredHealth() {
		return ModConfig.get().event(HordeEvent.ID).getDouble("warlordHealth");
	}

	/** While it is after someone, the warlord lets out a war cry every few seconds. */
	private static void act(ServerLevel level, Mob warlord, int tick) {
		if (Creature.everySeconds(warlord, tick, WAR_CRY_SECONDS) && warlord.getTarget() != null) {
			warCry(level, warlord);
		}
	}

	/** A roar that gives the monsters around the warlord Strength and Speed for a few seconds. */
	public static void warCry(ServerLevel level, Mob warlord) {
		level.playSound(null, warlord.getX(), warlord.getY(), warlord.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0f, 0.8f);
		level.sendParticles(ParticleTypes.ANGRY_VILLAGER, warlord.getX(), warlord.getY() + warlord.getBbHeight(), warlord.getZ(), 6, 0.5, 0.3, 0.5, 0.0);
		for (Mob mob : level.getEntitiesOfClass(Mob.class, warlord.getBoundingBox().inflate(WAR_CRY_RANGE),
				mob -> mob != warlord && mob.isAlive() && mob instanceof Enemy)) {
			spurOn(level, mob);
		}
	}

	private static void spurOn(ServerLevel level, LivingEntity mob) {
		mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, WAR_CRY_TICKS, 0));
		mob.addEffect(new MobEffectInstance(MobEffects.SPEED, WAR_CRY_TICKS, 0));
		level.sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
	}

	public static void dropLoot(ServerLevel level, Mob warlord, ServerPlayer killer, double axeChance) {
		RandomSource random = level.getRandom();
		Vec3 at = warlord.position().add(0.0, 0.5, 0.0);
		if (random.nextDouble() < axeChance) {
			Drops.spawn(level, at, EventItems.WARLORD_AXE.create(Lang.language(killer), 1));
		}
		Drops.spawn(level, at, new ItemStack(Items.EMERALD, Mth.nextInt(random, 4, 8)));
		ExperienceOrb.award(level, at, XP_REWARD);
	}
}
