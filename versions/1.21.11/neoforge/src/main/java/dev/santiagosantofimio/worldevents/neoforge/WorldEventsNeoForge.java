package dev.santiagosantofimio.worldevents.neoforge;

import com.mojang.serialization.MapCodec;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.command.WorldEventsCommand;
import dev.santiagosantofimio.worldevents.platform.Hooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** NeoForge entry point: sets the mod up and forwards NeoForge events to {@link Hooks}. */
@Mod(WorldEventsMod.MOD_ID)
public class WorldEventsNeoForge {
	private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
			DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, WorldEventsMod.MOD_ID);

	static {
		LOOT_MODIFIERS.register("drops", () -> HookLootModifier.CODEC);
	}

	public WorldEventsNeoForge(IEventBus modBus) {
		LOOT_MODIFIERS.register(modBus);
		WorldEventsMod.init();

		IEventBus bus = NeoForge.EVENT_BUS;
		bus.addListener((ServerTickEvent.Post event) -> Hooks.endServerTick(event.getServer()));
		bus.addListener((LevelTickEvent.Post event) -> {
			if (event.getLevel() instanceof ServerLevel level) {
				Hooks.endLevelTick(level);
			}
		});
		bus.addListener((ServerStoppedEvent event) -> Hooks.serverStopped(event.getServer()));
		bus.addListener(EventPriority.LOWEST, (EntityJoinLevelEvent event) -> {
			if (event.getLevel() instanceof ServerLevel level) {
				Hooks.entityLoad(event.getEntity(), level);
			}
		});
		bus.addListener((LivingIncomingDamageEvent event) -> {
			if (!event.getEntity().level().isClientSide() && !Hooks.allowDamage(event.getEntity(), event.getSource(), event.getAmount())) {
				event.setCanceled(true);
			}
		});
		bus.addListener((LivingDamageEvent.Post event) -> {
			if (!event.getEntity().level().isClientSide()) {
				Hooks.afterDamage(event.getEntity(), event.getSource(), event.getOriginalDamage(), event.getNewDamage(),
						event.getBlockedDamage() > 0);
			}
		});
		bus.addListener(EventPriority.LOWEST, (LivingDeathEvent event) -> {
			if (!event.getEntity().level().isClientSide()) {
				Hooks.afterDeath(event.getEntity(), event.getSource());
			}
		});
		bus.addListener((PlayerInteractEvent.RightClickItem event) -> {
			InteractionResult result = Hooks.useItem(event.getEntity(), event.getLevel(), event.getHand());
			if (result != InteractionResult.PASS) {
				event.setCanceled(true);
				event.setCancellationResult(result);
			}
		});
		bus.addListener((PlayerInteractEvent.RightClickBlock event) -> {
			InteractionResult result = Hooks.useBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
			if (result != InteractionResult.PASS) {
				event.setCanceled(true);
				event.setCancellationResult(result);
			}
		});
		bus.addListener((PlayerInteractEvent.EntityInteract event) -> {
			InteractionResult result = Hooks.useEntity(event.getEntity(), event.getLevel(), event.getHand(), event.getTarget(), null);
			if (result != InteractionResult.PASS) {
				event.setCanceled(true);
				event.setCancellationResult(result);
			}
		});
		bus.addListener((BlockEvent.BreakEvent event) -> {
			if (event.getLevel() instanceof Level level && !Hooks.beforeBlockBreak(level, event.getPlayer(), event.getPos(),
					event.getState(), level.getBlockEntity(event.getPos()))) {
				event.setCanceled(true);
			}
		});
		bus.addListener((CanPlayerSleepEvent event) -> {
			Player.BedSleepingProblem problem = Hooks.allowSleeping(event.getEntity(), event.getPos());
			if (problem != null) {
				event.setProblem(problem);
			}
		});
		bus.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				Hooks.playerJoin(player);
			}
		});
		bus.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				Hooks.playerLeave(player);
			}
		});
		bus.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				Hooks.playerChangedLevel(player);
			}
		});
		bus.addListener((RegisterCommandsEvent event) -> WorldEventsCommand.register(event.getDispatcher()));
	}
}
