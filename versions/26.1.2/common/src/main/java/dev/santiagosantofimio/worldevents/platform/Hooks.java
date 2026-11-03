package dev.santiagosantofimio.worldevents.platform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * The game events the mod listens to, independent of the mod loader. Each loader module forwards its own events here
 * (Fabric API callbacks, NeoForge or Forge events), and the common code registers its listeners on these.
 */
public final class Hooks {
	private Hooks() {
	}

	public static final Event<Consumer<MinecraftServer>> END_SERVER_TICK = new Event<>();
	public static final Event<Consumer<ServerLevel>> END_LEVEL_TICK = new Event<>();
	public static final Event<Consumer<MinecraftServer>> SERVER_STOPPED = new Event<>();
	public static final Event<EntityLoad> ENTITY_LOAD = new Event<>();
	public static final Event<AllowDamage> ALLOW_DAMAGE = new Event<>();
	public static final Event<AfterDamage> AFTER_DAMAGE = new Event<>();
	public static final Event<AfterDeath> AFTER_DEATH = new Event<>();
	public static final Event<UseItem> USE_ITEM = new Event<>();
	public static final Event<UseBlock> USE_BLOCK = new Event<>();
	public static final Event<UseEntity> USE_ENTITY = new Event<>();
	public static final Event<ModifyDrops> MODIFY_DROPS = new Event<>();
	public static final Event<BeforeBlockBreak> BEFORE_BLOCK_BREAK = new Event<>();
	public static final Event<AllowSleeping> ALLOW_SLEEPING = new Event<>();
	/** A player joins the server. */
	public static final Event<Consumer<ServerPlayer>> PLAYER_JOIN = new Event<>();
	/** A player leaves the server. */
	public static final Event<Consumer<ServerPlayer>> PLAYER_LEAVE = new Event<>();
	/** A player moves to another dimension. */
	public static final Event<Consumer<ServerPlayer>> PLAYER_CHANGED_LEVEL = new Event<>();

	/** Listeners, in the order they were registered. */
	public static final class Event<T> {
		private final List<T> listeners = new CopyOnWriteArrayList<>();

		public void register(T listener) {
			listeners.add(listener);
		}

		public List<T> listeners() {
			return listeners;
		}
	}

	@FunctionalInterface
	public interface EntityLoad {
		void onLoad(Entity entity, ServerLevel level);
	}

	@FunctionalInterface
	public interface AllowDamage {
		/** False cancels the damage. */
		boolean allowDamage(LivingEntity entity, DamageSource source, float amount);
	}

	@FunctionalInterface
	public interface AfterDamage {
		void afterDamage(LivingEntity entity, DamageSource source, float baseDamage, float damageTaken, boolean blocked);
	}

	@FunctionalInterface
	public interface AfterDeath {
		void afterDeath(LivingEntity entity, DamageSource source);
	}

	@FunctionalInterface
	public interface UseItem {
		InteractionResult interact(Player player, Level level, InteractionHand hand);
	}

	@FunctionalInterface
	public interface UseBlock {
		InteractionResult interact(Player player, Level level, InteractionHand hand, BlockHitResult hit);
	}

	@FunctionalInterface
	public interface UseEntity {
		InteractionResult interact(Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hit);
	}

	@FunctionalInterface
	public interface ModifyDrops {
		/** The drops of a loot table roll, which listeners may change. The first argument is unused (the table). */
		void modifyDrops(Object table, LootContext context, List<ItemStack> drops);
	}

	@FunctionalInterface
	public interface BeforeBlockBreak {
		/** False stops the player from breaking the block. */
		boolean beforeBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity);
	}

	@FunctionalInterface
	public interface AllowSleeping {
		/** A problem stops the player from sleeping; null lets them. */
		Player.BedSleepingProblem allowSleep(Player player, BlockPos pos);
	}

	// Dispatch, called by the loader modules.

	public static void endServerTick(MinecraftServer server) {
		END_SERVER_TICK.listeners.forEach(l -> l.accept(server));
	}

	public static void endLevelTick(ServerLevel level) {
		END_LEVEL_TICK.listeners.forEach(l -> l.accept(level));
	}

	public static void serverStopped(MinecraftServer server) {
		SERVER_STOPPED.listeners.forEach(l -> l.accept(server));
	}

	public static void entityLoad(Entity entity, ServerLevel level) {
		ENTITY_LOAD.listeners.forEach(l -> l.onLoad(entity, level));
	}

	public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		for (AllowDamage listener : ALLOW_DAMAGE.listeners) {
			if (!listener.allowDamage(entity, source, amount)) {
				return false;
			}
		}
		return true;
	}

	public static void afterDamage(LivingEntity entity, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
		AFTER_DAMAGE.listeners.forEach(l -> l.afterDamage(entity, source, baseDamage, damageTaken, blocked));
	}

	public static void afterDeath(LivingEntity entity, DamageSource source) {
		AFTER_DEATH.listeners.forEach(l -> l.afterDeath(entity, source));
	}

	public static InteractionResult useItem(Player player, Level level, InteractionHand hand) {
		for (UseItem listener : USE_ITEM.listeners) {
			InteractionResult result = listener.interact(player, level, hand);
			if (result != InteractionResult.PASS) {
				return result;
			}
		}
		return InteractionResult.PASS;
	}

	public static InteractionResult useBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		for (UseBlock listener : USE_BLOCK.listeners) {
			InteractionResult result = listener.interact(player, level, hand, hit);
			if (result != InteractionResult.PASS) {
				return result;
			}
		}
		return InteractionResult.PASS;
	}

	public static InteractionResult useEntity(Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hit) {
		for (UseEntity listener : USE_ENTITY.listeners) {
			InteractionResult result = listener.interact(player, level, hand, entity, hit);
			if (result != InteractionResult.PASS) {
				return result;
			}
		}
		return InteractionResult.PASS;
	}

	public static void modifyDrops(LootContext context, List<ItemStack> drops) {
		MODIFY_DROPS.listeners.forEach(l -> l.modifyDrops(null, context, drops));
	}

	public static boolean beforeBlockBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		for (BeforeBlockBreak listener : BEFORE_BLOCK_BREAK.listeners) {
			if (!listener.beforeBreak(level, player, pos, state, blockEntity)) {
				return false;
			}
		}
		return true;
	}

	public static Player.BedSleepingProblem allowSleeping(Player player, BlockPos pos) {
		for (AllowSleeping listener : ALLOW_SLEEPING.listeners) {
			Player.BedSleepingProblem problem = listener.allowSleep(player, pos);
			if (problem != null) {
				return problem;
			}
		}
		return null;
	}

	public static void playerJoin(ServerPlayer player) {
		PLAYER_JOIN.listeners.forEach(l -> l.accept(player));
	}

	public static void playerLeave(ServerPlayer player) {
		PLAYER_LEAVE.listeners.forEach(l -> l.accept(player));
	}

	public static void playerChangedLevel(ServerPlayer player) {
		PLAYER_CHANGED_LEVEL.listeners.forEach(l -> l.accept(player));
	}
}
