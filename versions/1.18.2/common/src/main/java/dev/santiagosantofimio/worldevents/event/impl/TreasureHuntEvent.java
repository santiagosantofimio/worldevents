package dev.santiagosantofimio.worldevents.event.impl;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldevents.config.EventSettings;
import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.WorldEvent;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.mob.Creature;
import dev.santiagosantofimio.worldevents.mob.MobSpawning;
import dev.santiagosantofimio.worldevents.mob.SpectralCaptain;
import dev.santiagosantofimio.worldevents.platform.Hooks;
import dev.santiagosantofimio.worldevents.util.Advancements;
import dev.santiagosantofimio.worldevents.util.DayCycle;
import dev.santiagosantofimio.worldevents.util.DeferredDiscard;
import dev.santiagosantofimio.worldevents.util.Particles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;

/**
 * A treasure hunt: a chest full of loot appears on the surface somewhere near players, and whoever gets to it first
 * takes it. The action bar tells each player how warm they are, and a faint column of light rises from the chest. A
 * spectral captain rises to guard it when someone comes close, and the chest stays shut until the captain falls.
 *
 * <p>The chest only takes the place of air or plants. If nobody finds it, it goes away with the event; if its chunk
 * is not loaded by then, it stays there for someone to stumble upon.
 */
public final class TreasureHuntEvent implements WorldEvent {
	public static final String ID = "treasure_hunt";
	private static final String CHEST = "chest";
	private static final String CAPTAIN_RAISED = "captainRaised";
	private static final String CAPTAIN_DOWN = "captainDown";
	private static final String CLAIMED = "claimed";
	private static final String LOST = "lost";
	private static final long NO_CHEST = Long.MIN_VALUE;
	private static final long EARLIEST_START = 1000;
	private static final long LATEST_START = 9000;
	private static final int MIN_CHEST_DISTANCE = 32;
	private static final int MAX_CHEST_DISTANCE = 64;
	/** The captain rises when someone comes this close to the chest. */
	private static final double GUARD_RANGE = 10.0;
	private static final double WARM_RANGE = 64.0;
	private static final double HOT_RANGE = 32.0;
	private static final double BURNING_RANGE = 16.0;
	private static final double LUNGE_STRENGTH = 1.6;
	private static final int LUNGE_COOLDOWN_SECONDS = 8;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public JsonObject defaultSettings() {
		// At most once every 5 in-game days; lasts 8 minutes, or until someone finds the treasure.
		JsonObject json = EventSettings.defaults(true, 8, 6000, 480);
		json.addProperty("guardian", true);
		json.addProperty("captainHealth", 120.0);
		json.addProperty("cutlassChance", 0.35);
		return json;
	}

	@Override
	public BossEvent.BossBarColor barColor() {
		return BossEvent.BossBarColor.BLUE;
	}

	@Override
	public ChatFormatting textColor() {
		return ChatFormatting.DARK_AQUA;
	}

	@Override
	public SoundEvent startSound() {
		return SoundEvents.UI_TOAST_CHALLENGE_COMPLETE;
	}

	/** Needs a surface and daylight: the overworld, not the Nether or the End. */
	@Override
	public boolean isAvailable(ServerLevel level) {
		return DayCycle.hasDayCycle(level);
	}

	/** In the morning, so the hunt happens in daylight. */
	@Override
	public boolean canStart(ServerLevel level) {
		return !level.isNight() && DayCycle.isBetween(level, EARLIEST_START, LATEST_START);
	}

	@Override
	public boolean shouldEndEarly(ServerLevel level, ActiveEvent active) {
		return active.data().getBoolean(CLAIMED) || active.data().getBoolean(LOST);
	}

	@Override
	public void tick(ServerLevel level, ActiveEvent active) {
		BlockPos chest = treasure(active);
		if (chest == null) {
			hideTreasure(level, active);
			return;
		}
		boolean loaded = level.isLoaded(chest);
		if (loaded && !level.getBlockState(chest).is(Blocks.CHEST)) {
			active.data().putBoolean(LOST, true);
			return;
		}
		int elapsed = (active.totalTicks() - active.remainingTicks()) / 20;
		if (loaded && elapsed % 3 == 0) {
			Particles.far(level, ParticleTypes.END_ROD, chest.getX() + 0.5, chest.getY() + 4.0, chest.getZ() + 0.5, 16, 0.1, 3.0, 0.1, 0.01);
		}
		if (elapsed % 2 == 0) {
			for (ServerPlayer player : level.players()) {
				if (!player.isSpectator()) {
					hint(player, chest);
				}
			}
		}
		HuntState state = active.state(HuntState::new);
		if (state.captain != null && (state.captain.isRemoved() || !state.captain.isAlive())) {
			state.captain = null;
		}
		if (guarded(level, active) && !active.data().getBoolean(CAPTAIN_RAISED)) {
			for (ServerPlayer player : level.players()) {
				if (!player.isSpectator() && player.distanceToSqr(Vec3.atCenterOf(chest)) < GUARD_RANGE * GUARD_RANGE) {
					raiseCaptain(level, active, chest, Lang.language(player));
					break;
				}
			}
		}
	}

	@Override
	public void end(ServerLevel level, ActiveEvent active) {
		HuntState state = active.state(HuntState::new);
		if (state.captain != null && !state.captain.isRemoved()) {
			level.sendParticles(ParticleTypes.SOUL, state.captain.getX(), state.captain.getY() + 1.0, state.captain.getZ(), 30, 0.5, 1.0, 0.5, 0.02);
			Creature.remove(state.captain);
		}
		state.captain = null;
		BlockPos chest = treasure(active);
		if (chest != null && !active.data().getBoolean(CLAIMED) && level.isLoaded(chest)
				&& level.getBlockEntity(chest) instanceof ChestBlockEntity treasure) {
			treasure.clearContent();
			level.removeBlock(chest, false);
			level.sendParticles(ParticleTypes.POOF, chest.getX() + 0.5, chest.getY() + 0.5, chest.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.02);
		}
	}

	/** Where the chest is, or {@code null} if it has not appeared yet. */
	public static BlockPos treasure(ActiveEvent active) {
		long chest = active.data().contains(CHEST) ? active.data().getLong(CHEST) : NO_CHEST;
		return chest == NO_CHEST ? null : BlockPos.of(chest);
	}

	private void hideTreasure(ServerLevel level, ActiveEvent active) {
		List<ServerPlayer> hunters = new ArrayList<>();
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator()) {
				hunters.add(player);
			}
		}
		if (hunters.isEmpty()) {
			return;
		}
		ServerPlayer near = hunters.get(level.getRandom().nextInt(hunters.size()));
		BlockPos spot = MobSpawning.surfaceSpot(level, EntityType.VINDICATOR, near.blockPosition(), MIN_CHEST_DISTANCE, MAX_CHEST_DISTANCE, 8);
		if (spot != null && level.getBlockState(spot).getMaterial().isReplaceable()) {
			placeTreasure(level, active, spot, Lang.language(near));
			for (ServerPlayer player : level.players()) {
				player.sendMessage(Lang.component(player, "event.treasure_hunt.hidden").withStyle(textColor()), net.minecraft.Util.NIL_UUID);
			}
		}
	}

	/** Puts the chest at {@code pos}, with its loot named in {@code language}. Also for tests and admins. */
	public void placeTreasure(ServerLevel level, ActiveEvent active, BlockPos pos, String language) {
		level.setBlock(pos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
		if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
			fill(chest, level.getRandom(), language);
		}
		active.data().putLong(CHEST, pos.asLong());
	}

	private static void fill(ChestBlockEntity chest, Random random, String language) {
		List<ItemStack> loot = new ArrayList<>(List.of(
				new ItemStack(Items.EMERALD, Mth.nextInt(random, 6, 14)),
				new ItemStack(Items.GOLD_INGOT, Mth.nextInt(random, 3, 8)),
				new ItemStack(Items.DIAMOND, Mth.nextInt(random, 1, 3)),
				EventItems.EVENT_SEAL.create(language, 2)));
		List<EventItems.EventItem> treasures = new ArrayList<>(List.of(EventItems.METEORITE_FRAGMENT, EventItems.LUCKY_NUGGET,
				EventItems.GOLDEN_SEED, EventItems.BOTTLED_LIGHTNING, EventItems.MAGMA_HEART, EventItems.UNSTABLE_PEARL));
		Collections.shuffle(treasures, random);
		loot.add(treasures.get(0).create(language, 1));
		loot.add(treasures.get(1).create(language, 1));
		List<Integer> slots = new ArrayList<>();
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			slots.add(slot);
		}
		Collections.shuffle(slots, random);
		for (int i = 0; i < loot.size(); i++) {
			chest.setItem(slots.get(i), loot.get(i));
		}
	}

	/** How warm the player is: a nudge in the right direction, without giving the chest away. */
	private static void hint(ServerPlayer player, BlockPos chest) {
		double distance = Math.sqrt(player.distanceToSqr(Vec3.atCenterOf(chest)));
		String key = distance < BURNING_RANGE ? "burning" : distance < HOT_RANGE ? "hot" : distance < WARM_RANGE ? "warm" : "cold";
		ChatFormatting color = distance < BURNING_RANGE ? ChatFormatting.RED : distance < HOT_RANGE ? ChatFormatting.GOLD
				: distance < WARM_RANGE ? ChatFormatting.YELLOW : ChatFormatting.AQUA;
		player.displayClientMessage(Lang.component(player, "event.treasure_hunt.hint." + key).withStyle(color), true);
	}

	private void raiseCaptain(ServerLevel level, ActiveEvent active, BlockPos chest, String language) {
		BlockPos spot = MobSpawning.spotNear(level, EntityType.VINDICATOR, chest, 2, 5, 3, 8);
		Vindicator captain = SpectralCaptain.spawn(level, spot != null ? spot : chest.above(), language, settings().getDouble("captainHealth"));
		if (captain == null) {
			return;
		}
		active.state(HuntState::new).captain = captain;
		active.data().putBoolean(CAPTAIN_RAISED, true);
		level.playSound(null, captain.getX(), captain.getY(), captain.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 2.0f, 1.0f);
		for (ServerPlayer player : level.players()) {
			player.sendMessage(Lang.component(player, "mob.spectral_captain.arrived").withStyle(ChatFormatting.AQUA), net.minecraft.Util.NIL_UUID);
		}
	}

	/** The captain of the current run, if it is loaded and alive. */
	public static Vindicator captain(ActiveEvent active) {
		Vindicator captain = active.state(HuntState::new).captain;
		return captain != null && captain.isAlive() ? captain : null;
	}

	/** Whether the captain still keeps players away from the chest. Monsters do not exist on peaceful. */
	private boolean guarded(ServerLevel level, ActiveEvent active) {
		return settings().getBoolean("guardian") && level.getDifficulty() != Difficulty.PEACEFUL && !active.data().getBoolean(CAPTAIN_DOWN);
	}

	/** Whether {@code pos} opens the treasure: the chest itself, or a chest placed next to it to make a double one. */
	private static boolean isTreasure(ServerLevel level, ActiveEvent active, BlockPos pos) {
		BlockPos chest = treasure(active);
		if (chest == null) {
			return false;
		}
		BlockState state = level.getBlockState(pos);
		return pos.equals(chest) || state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
				&& pos.relative(ChestBlock.getConnectedDirection(state)).equals(chest);
	}

	private static void claim(ServerLevel level, ActiveEvent active, ServerPlayer finder) {
		CompoundTag data = active.data();
		data.putBoolean(CLAIMED, true);
		EventManager.score(finder, ID, 10);
		Advancements.award(finder, "find_treasure");
		for (ServerPlayer player : level.players()) {
			player.sendMessage(Lang.component(player, "event.treasure_hunt.found", finder.getName().getString()).withStyle(ChatFormatting.GOLD), net.minecraft.Util.NIL_UUID);
		}
	}

	@Override
	public void registerHooks() {
		Hooks.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof Vindicator captain) || !SpectralCaptain.is(captain) || Creature.isSummoned(captain)) {
				return;
			}
			ActiveEvent active = EventManager.active(level);
			if (active != null && active.event() == this) {
				HuntState state = active.state(HuntState::new);
				if (state.captain == null || state.captain.isRemoved()) {
					state.captain = captain;
				}
			} else {
				DeferredDiscard.discard(captain);
				captain.getPassengers().forEach(DeferredDiscard::discard);
			}
		});

		Hooks.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof Mob captain) || !SpectralCaptain.is(captain) || !(captain.level instanceof ServerLevel level)) {
				return;
			}
			ServerPlayer killer = source.getEntity() instanceof ServerPlayer player ? player
					: captain.getKillCredit() instanceof ServerPlayer player ? player : null;
			ActiveEvent active = EventManager.active(level);
			if (active != null && active.event() == this && !Creature.isSummoned(captain)) {
				active.data().putBoolean(CAPTAIN_DOWN, true);
				for (ServerPlayer player : level.players()) {
					player.sendMessage(Lang.component(player, "event.treasure_hunt.free").withStyle(ChatFormatting.GOLD), net.minecraft.Util.NIL_UUID);
				}
				if (killer != null) {
					EventManager.score(killer, ID, 10);
				}
			}
			if (killer != null) {
				SpectralCaptain.dropLoot(level, captain, killer, settings().getDouble("cutlassChance"));
				Advancements.award(killer, "defeat_spectral_captain");
			}
		});

		// The first one to open the chest ends the hunt; while the captain stands, it stays shut.
		Hooks.USE_BLOCK.register((player, world, hand, hit) -> {
			if (!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer hunter) || player.isSpectator()) {
				return InteractionResult.PASS;
			}
			ActiveEvent active = EventManager.active(level);
			if (active == null || active.event() != this || active.data().getBoolean(CLAIMED)
					|| !isTreasure(level, active, hit.getBlockPos())) {
				return InteractionResult.PASS;
			}
			if (guarded(level, active)) {
				if (!active.data().getBoolean(CAPTAIN_RAISED)) {
					raiseCaptain(level, active, treasure(active), Lang.language(hunter));
				}
				hunter.displayClientMessage(Lang.component(hunter, "event.treasure_hunt.guarded").withStyle(ChatFormatting.AQUA), true);
				return InteractionResult.FAIL;
			}
			claim(level, active, hunter);
			return InteractionResult.PASS;
		});

		// Nobody gets the loot by breaking the chest before the hunt is over.
		Hooks.BEFORE_BLOCK_BREAK.register((world, player, pos, state, blockEntity) -> {
			if (!(world instanceof ServerLevel level)) {
				return true;
			}
			ActiveEvent active = EventManager.active(level);
			return active == null || active.event() != this || active.data().getBoolean(CLAIMED) || !pos.equals(treasure(active));
		});

		// The captain's cutlass: right-click to lunge forward.
		Hooks.USE_ITEM.register((player, world, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!EventItems.CAPTAINS_CUTLASS.is(stack) || !(player instanceof ServerPlayer) || !(world instanceof ServerLevel level)
					|| player.getCooldowns().isOnCooldown(stack.getItem())) {
				return InteractionResult.PASS;
			}
			Vec3 look = player.getLookAngle();
			player.setDeltaMovement(look.x * LUNGE_STRENGTH, Math.max(look.y * LUNGE_STRENGTH / 2.0, 0.3), look.z * LUNGE_STRENGTH);
			player.hurtMarked = true;
			level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.5, player.getZ(), 10, 0.3, 0.2, 0.3, 0.02);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);
			player.getCooldowns().addCooldown(stack.getItem(), LUNGE_COOLDOWN_SECONDS * 20);
			return InteractionResult.SUCCESS;
		});
	}

	private static final class HuntState {
		private Vindicator captain;
	}
}
