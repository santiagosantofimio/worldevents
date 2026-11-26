package dev.santiagosantofimio.worldomens.event.impl;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldomens.WorldOmensMod;
import dev.santiagosantofimio.worldomens.i18n.Lang;
import dev.santiagosantofimio.worldomens.item.EventItems;
import dev.santiagosantofimio.worldomens.mob.Creature;
import dev.santiagosantofimio.worldomens.mob.Tidecaller;
import dev.santiagosantofimio.worldomens.util.DayCycle;
import dev.santiagosantofimio.worldomens.util.Protection;
import java.util.List;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

public final class DrownedTideEvent extends AssaultEvent {
	public static final String ID = "drowned_tide";
	public static final String MOB_TAG = WorldOmensMod.MOD_ID + ".tide";
	private static final long LATEST_START = 17000;
	private static final int COAST_MIN_DISTANCE = 6;
	private static final int COAST_MAX_DISTANCE = 22;
	private static final int COAST_ATTEMPTS = 24;
	private static final int BLESSING_TICKS = 900;

	private static final List<Attacker> ATTACKERS = List.of(new Attacker(EntityTypes.DROWNED, 1, 1));

	@Override
	public String id() {
		return ID;
	}

	@Override
	public JsonObject defaultSettings() {
		JsonObject json = assaultDefaults(8, 6000, 600);
		json.addProperty("bossHealth", 180.0);
		return json;
	}

	@Override
	public BossEvent.BossBarColor barColor() {
		return BossEvent.BossBarColor.BLUE;
	}

	@Override
	public ChatFormatting textColor() {
		return ChatFormatting.AQUA;
	}

	@Override
	public Holder<SoundEvent> startSound() {
		return SoundEvents.RAID_HORN;
	}

	@Override
	public boolean canStart(ServerLevel level) {
		if (!level.isDarkOutside() || !DayCycle.isBetween(level, DayCycle.NIGHT_START, LATEST_START)) {
			return false;
		}
		for (ServerPlayer player : fighters(level)) {
			if (isOnCoast(level, player)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isOnCoast(ServerLevel level, ServerPlayer player) {
		return !player.isInWater() && waterSpot(level, player.blockPosition(), COAST_MIN_DISTANCE, COAST_MAX_DISTANCE, COAST_ATTEMPTS) != null;
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
		return Tidecaller.KIND;
	}

	@Override
	protected EntityType<? extends Mob> bossType() {
		return EntityTypes.DROWNED;
	}

	@Override
	protected Mob spawnBoss(ServerLevel level, BlockPos pos, String language, double health) {
		return Tidecaller.spawn(level, pos, language, health);
	}

	@Override
	protected void dropBossLoot(ServerLevel level, Mob boss, ServerPlayer killer, double relicChance) {
		Tidecaller.dropLoot(level, boss, killer, relicChance);
	}

	@Override
	protected String victoryAdvancement() {
		return "survive_drowned_tide";
	}

	@Override
	protected String bossAdvancement() {
		return "defeat_tidecaller";
	}

	@Override
	protected SoundEvent bossSound() {
		return SoundEvents.ELDER_GUARDIAN_CURSE;
	}

	@Override
	protected boolean startsAtNight() {
		return true;
	}

	@Override
	protected boolean allowedHere(ServerLevel level) {
		return level.dimension() == Level.OVERWORLD && DayCycle.hasDayCycle(level);
	}

	@Override
	protected BlockPos findSpot(ServerLevel level, EntityType<? extends Mob> type, ServerPlayer target, int minDistance, int maxDistance) {
		return waterSpot(level, target.blockPosition(), minDistance, maxDistance, COAST_ATTEMPTS);
	}

	@Override
	protected void prepare(Mob mob) {
		if (mob instanceof Zombie zombie) {
			zombie.setCanBreakDoors(false);
		}
	}

	public static BlockPos waterSpot(ServerLevel level, BlockPos center, int minDistance, int maxDistance, int attempts) {
		RandomSource random = level.getRandom();
		for (int i = 0; i < attempts; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = Mth.nextDouble(random, minDistance, maxDistance);
			int x = Mth.floor(center.getX() + Math.cos(angle) * distance);
			int z = Mth.floor(center.getZ() + Math.sin(angle) * distance);
			if (!level.isPositionEntityTicking(new BlockPos(x, center.getY(), z))) {
				continue;
			}
			int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
			BlockPos surface = new BlockPos(x, top, z);
			BlockPos spot = surface.below();
			if (level.getFluidState(surface).is(FluidTags.WATER) && level.getFluidState(spot).is(FluidTags.WATER)
					&& !Protection.isSpawnProtected(level, spot) && level.getWorldBorder().isWithinBounds(spot)) {
				return spot;
			}
		}
		return null;
	}

	private static void bless(ServerLevel level, ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, BLESSING_TICKS, 0));
		player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, BLESSING_TICKS, 0));
		player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, BLESSING_TICKS, 0));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.5f, 1.0f);
		player.sendSystemMessage(Lang.component(player, "item.tidecallers_conch.blessing").withStyle(ChatFormatting.AQUA), true);
	}

	@Override
	public void registerHooks() {
		super.registerHooks();
		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!EventItems.TIDECALLERS_CONCH.is(stack) || !(player instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel level)
					|| player.getCooldowns().isOnCooldown(stack)) {
				return InteractionResult.PASS;
			}
			bless(level, serverPlayer);
			player.getCooldowns().addCooldown(EventItems.TIDAL_BLESSING, EventItems.TIDAL_BLESSING_COOLDOWN_SECONDS * 20);
			return InteractionResult.SUCCESS;
		});
	}
}
