package dev.santiagosantofimio.worldevents.platform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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

/**
 * The game events the mod listens to. Each mod loader (Fabric, NeoForge, Forge) feeds them from its own events, so
 * the rest of the mod only ever sees these. Listeners run in the order they were registered.
 */
public final class GameEvents {
	/** End of every server tick. */
	public static final Event<ServerTick> SERVER_TICK = new Event<>();
	/** End of every tick of each dimension. */
	public static final Event<LevelTick> LEVEL_TICK = new Event<>();
	public static final Event<ServerStopped> SERVER_STOPPED = new Event<>();
	/** An entity enters a level: spawned, or loaded with its chunk. */
	public static final Event<EntityLoad> ENTITY_LOAD = new Event<>();
	/** Before a living entity takes damage; any listener can cancel it. */
	public static final Event<AllowDamage> ALLOW_DAMAGE = new Event<>();
	public static final Event<AfterDamage> AFTER_DAMAGE = new Event<>();
	public static final Event<AfterDeath> AFTER_DEATH = new Event<>();
	/** A player uses the item in their hand on nothing in particular. The first answer other than PASS wins. */
	public static final Event<UseItem> USE_ITEM = new Event<>();
	/** A player right-clicks a block. The first answer other than PASS wins. */
	public static final Event<UseBlock> USE_BLOCK = new Event<>();
	/** A player right-clicks an entity. The first answer other than PASS wins. */
	public static final Event<UseEntity> USE_ENTITY = new Event<>();
	/** Before a player breaks a block; any listener can stop it. */
	public static final Event<BeforeBlockBreak> BEFORE_BLOCK_BREAK = new Event<>();
	/** Whatever a loot table has just rolled (a broken block, a dead mob…), before it is handed out. */
	public static final Event<ModifyDrops> MODIFY_DROPS = new Event<>();
	/** Whether a player may sleep; a listener can answer with the reason they may not. */
	public static final Event<AllowSleeping> ALLOW_SLEEPING = new Event<>();
	public static final Event<PlayerEvent> PLAYER_JOIN = new Event<>();
	public static final Event<PlayerEvent> PLAYER_LEAVE = new Event<>();
	public static final Event<PlayerEvent> PLAYER_CHANGE_LEVEL = new Event<>();

	private GameEvents() {
	}

	/** A list of listeners. */
	public static final class Event<T> {
		private final List<T> listeners = new CopyOnWriteArrayList<>();

		public void register(T listener) {
			listeners.add(listener);
		}

		public List<T> listeners() {
			return listeners;
		}
	}

	public interface ServerTick {
		void onTick(MinecraftServer server);
	}

	public interface LevelTick {
		void onTick(ServerLevel level);
	}

	public interface ServerStopped {
		void onStopped(MinecraftServer server);
	}

	public interface EntityLoad {
		void onLoad(Entity entity, ServerLevel level);
	}

	public interface AllowDamage {
		boolean allow(LivingEntity entity, DamageSource source, float amount);
	}

	public interface AfterDamage {
		void afterDamage(LivingEntity entity, DamageSource source, float baseDamage, float damageTaken, boolean blocked);
	}

	public interface AfterDeath {
		void afterDeath(LivingEntity entity, DamageSource source);
	}

	public interface UseItem {
		InteractionResult use(Player player, Level level, InteractionHand hand);
	}

	public interface UseBlock {
		InteractionResult use(Player player, Level level, InteractionHand hand, BlockHitResult hit);
	}

	public interface UseEntity {
		InteractionResult use(Player player, Level level, InteractionHand hand, Entity entity);
	}

	public interface BeforeBlockBreak {
		boolean allow(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity);
	}

	public interface ModifyDrops {
		void modify(LootContext context, List<ItemStack> drops);
	}

	public interface AllowSleeping {
		/** {@code null} lets the player sleep. */
		Player.BedSleepingProblem check(Player player, BlockPos pos);
	}

	public interface PlayerEvent {
		void on(ServerPlayer player);
	}

	public static void serverTick(MinecraftServer server) {
		SERVER_TICK.listeners().forEach(listener -> listener.onTick(server));
	}

	public static void levelTick(ServerLevel level) {
		LEVEL_TICK.listeners().forEach(listener -> listener.onTick(level));
	}

	public static void serverStopped(MinecraftServer server) {
		SERVER_STOPPED.listeners().forEach(listener -> listener.onStopped(server));
	}

	public static void entityLoad(Entity entity, ServerLevel level) {
		ENTITY_LOAD.listeners().forEach(listener -> listener.onLoad(entity, level));
	}

	public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		for (AllowDamage listener : ALLOW_DAMAGE.listeners()) {
			if (!listener.allow(entity, source, amount)) {
				return false;
			}
		}
		return true;
	}

	public static void afterDamage(LivingEntity entity, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
		AFTER_DAMAGE.listeners().forEach(listener -> listener.afterDamage(entity, source, baseDamage, damageTaken, blocked));
	}

	public static void afterDeath(LivingEntity entity, DamageSource source) {
		AFTER_DEATH.listeners().forEach(listener -> listener.afterDeath(entity, source));
	}

	public static InteractionResult useItem(Player player, Level level, InteractionHand hand) {
		for (UseItem listener : USE_ITEM.listeners()) {
			InteractionResult result = listener.use(player, level, hand);
			if (result != InteractionResult.PASS) {
				return result;
			}
		}
		return InteractionResult.PASS;
	}

	public static InteractionResult useBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		for (UseBlock listener : USE_BLOCK.listeners()) {
			InteractionResult result = listener.use(player, level, hand, hit);
			if (result != InteractionResult.PASS) {
				return result;
			}
		}
		return InteractionResult.PASS;
	}

	public static InteractionResult useEntity(Player player, Level level, InteractionHand hand, Entity entity) {
		for (UseEntity listener : USE_ENTITY.listeners()) {
			InteractionResult result = listener.use(player, level, hand, entity);
			if (result != InteractionResult.PASS) {
				return result;
			}
		}
		return InteractionResult.PASS;
	}

	public static boolean beforeBlockBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		for (BeforeBlockBreak listener : BEFORE_BLOCK_BREAK.listeners()) {
			if (!listener.allow(level, player, pos, state, blockEntity)) {
				return false;
			}
		}
		return true;
	}

	public static void modifyDrops(LootContext context, List<ItemStack> drops) {
		MODIFY_DROPS.listeners().forEach(listener -> listener.modify(context, drops));
	}

	public static Player.BedSleepingProblem allowSleeping(Player player, BlockPos pos) {
		for (AllowSleeping listener : ALLOW_SLEEPING.listeners()) {
			Player.BedSleepingProblem problem = listener.check(player, pos);
			if (problem != null) {
				return problem;
			}
		}
		return null;
	}

	public static void playerJoin(ServerPlayer player) {
		PLAYER_JOIN.listeners().forEach(listener -> listener.on(player));
	}

	public static void playerLeave(ServerPlayer player) {
		PLAYER_LEAVE.listeners().forEach(listener -> listener.on(player));
	}

	public static void playerChangeLevel(ServerPlayer player) {
		PLAYER_CHANGE_LEVEL.listeners().forEach(listener -> listener.on(player));
	}
}
