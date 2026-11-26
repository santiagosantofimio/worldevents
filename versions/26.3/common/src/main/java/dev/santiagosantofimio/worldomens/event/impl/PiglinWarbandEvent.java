package dev.santiagosantofimio.worldomens.event.impl;

import dev.santiagosantofimio.worldomens.platform.Hooks;
import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldomens.WorldOmensMod;
import dev.santiagosantofimio.worldomens.i18n.Lang;
import dev.santiagosantofimio.worldomens.item.EventItems;
import dev.santiagosantofimio.worldomens.mob.Creature;
import dev.santiagosantofimio.worldomens.mob.MobSpawning;
import dev.santiagosantofimio.worldomens.mob.PiglinWarchief;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class PiglinWarbandEvent extends AssaultEvent {
	public static final String ID = "piglin_warband";
	public static final String MOB_TAG = WorldOmensMod.MOD_ID + ".warband";
	private static final int RESOLVE_TICKS = 600;
	private static final int ABSORPTION_AMPLIFIER = 1;

	private static final List<Attacker> ATTACKERS = List.of(
			new Attacker(EntityTypes.PIGLIN, 1, 5),
			new Attacker(EntityTypes.PIGLIN_BRUTE, 2, 3),
			new Attacker(EntityTypes.HOGLIN, 3, 2));

	@Override
	public String id() {
		return ID;
	}

	@Override
	public JsonObject defaultSettings() {
		JsonObject json = assaultDefaults(8, 6000, 600);
		json.addProperty("bossHealth", 160.0);
		return json;
	}

	@Override
	public BossEvent.BossBarColor barColor() {
		return BossEvent.BossBarColor.YELLOW;
	}

	@Override
	public ChatFormatting textColor() {
		return ChatFormatting.GOLD;
	}

	@Override
	public Holder<SoundEvent> startSound() {
		return SoundEvents.RAID_HORN;
	}

	@Override
	protected String mobTag() {
		return MOB_TAG;
	}

	@Override
	protected List<Attacker> attackers() {
		return ATTACKERS;
	}

	@Override
	protected Creature.Kind bossKind() {
		return PiglinWarchief.KIND;
	}

	@Override
	protected EntityType<? extends Mob> bossType() {
		return EntityTypes.PIGLIN_BRUTE;
	}

	@Override
	protected Mob spawnBoss(ServerLevel level, BlockPos pos, String language, double health) {
		return PiglinWarchief.spawn(level, pos, language, health);
	}

	@Override
	protected void dropBossLoot(ServerLevel level, Mob boss, ServerPlayer killer, double relicChance) {
		PiglinWarchief.dropLoot(level, boss, killer, relicChance);
	}

	@Override
	protected String victoryAdvancement() {
		return "survive_piglin_warband";
	}

	@Override
	protected String bossAdvancement() {
		return "defeat_piglin_warchief";
	}

	@Override
	protected SoundEvent bossSound() {
		return SoundEvents.PIGLIN_BRUTE_ANGRY;
	}

	@Override
	protected boolean startsAtNight() {
		return false;
	}

	@Override
	protected boolean allowedHere(ServerLevel level) {
		return level.dimension() == Level.NETHER;
	}

	@Override
	protected BlockPos findSpot(ServerLevel level, EntityType<? extends Mob> type, ServerPlayer target, int minDistance, int maxDistance) {
		return MobSpawning.spotNear(level, type, target.blockPosition(), minDistance, maxDistance, 6, 6);
	}

	@Override
	protected void prepare(Mob mob) {
		if (mob instanceof AbstractPiglin piglin) {
			piglin.setImmuneToZombification(true);
		}
		if (mob instanceof Hoglin hoglin) {
			hoglin.setImmuneToZombification(true);
		}
	}

	@Override
	protected void engage(Mob mob, ServerPlayer target) {
		if (mob.getClass() == Piglin.class && wearsGold(target)) {
			return;
		}
		mob.setTarget(target);
		if (mob instanceof Piglin piglin) {
			piglin.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
		} else if (mob instanceof PiglinBrute brute) {
			brute.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
		} else if (mob instanceof Hoglin hoglin) {
			hoglin.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
		}
	}

	public static boolean wearsGold(ServerPlayer player) {
		return player.getItemBySlot(EquipmentSlot.HEAD).is(Items.GOLDEN_HELMET)
				|| player.getItemBySlot(EquipmentSlot.CHEST).is(Items.GOLDEN_CHESTPLATE)
				|| player.getItemBySlot(EquipmentSlot.LEGS).is(Items.GOLDEN_LEGGINGS)
				|| player.getItemBySlot(EquipmentSlot.FEET).is(Items.GOLDEN_BOOTS);
	}

	private static void gildedResolve(ServerLevel level, ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, RESOLVE_TICKS, ABSORPTION_AMPLIFIER));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, RESOLVE_TICKS, 0));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PIGLIN_ADMIRING_ITEM, SoundSource.PLAYERS, 1.5f, 0.9f);
		player.sendSystemMessage(Lang.component(player, "item.warchiefs_cleaver.resolve").withStyle(ChatFormatting.GOLD), true);
	}

	@Override
	public void registerHooks() {
		super.registerHooks();
		Hooks.USE_ITEM.register((player, world, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!EventItems.WARCHIEFS_CLEAVER.is(stack) || !(player instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel level)
					|| player.getCooldowns().isOnCooldown(stack)) {
				return InteractionResult.PASS;
			}
			gildedResolve(level, serverPlayer);
			player.getCooldowns().addCooldown(EventItems.GILDED_RESOLVE, EventItems.GILDED_RESOLVE_COOLDOWN_SECONDS * 20);
			return InteractionResult.SUCCESS;
		});
	}
}
