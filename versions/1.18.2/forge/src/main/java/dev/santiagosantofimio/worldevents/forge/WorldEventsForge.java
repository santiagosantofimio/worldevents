package dev.santiagosantofimio.worldevents.forge;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.command.WorldEventsCommand;
import dev.santiagosantofimio.worldevents.platform.Hooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Forge entry point: sets the mod up and forwards Forge events to {@link Hooks}. */
@Mod(WorldEventsMod.MOD_ID)
public final class WorldEventsForge {
	private static final DeferredRegister<GlobalLootModifierSerializer<?>> LOOT_MODIFIERS =
			DeferredRegister.create(ForgeRegistries.Keys.LOOT_MODIFIER_SERIALIZERS, WorldEventsMod.MOD_ID);

	static {
		LOOT_MODIFIERS.register("drops", HookLootModifier.Serializer::new);
	}

	public WorldEventsForge() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		LOOT_MODIFIERS.register(modBus);
		if (FMLEnvironment.dist == Dist.CLIENT) {
			WorldEventsForgeClient.register();
		}
		WorldEventsMod.init();

		IEventBus bus = MinecraftForge.EVENT_BUS;
		bus.addListener((TickEvent.ServerTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				Hooks.endServerTick(ServerLifecycleHooks.getCurrentServer());
			}
		});
		bus.addListener((TickEvent.WorldTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END && event.world instanceof ServerLevel level) {
				Hooks.endLevelTick(level);
			}
		});
		bus.addListener((ServerStoppedEvent event) -> Hooks.serverStopped(event.getServer()));
		bus.addListener(EventPriority.LOWEST, (EntityJoinWorldEvent event) -> {
			if (!event.isCanceled() && event.getWorld() instanceof ServerLevel level) {
				Hooks.entityLoad(event.getEntity(), level);
			}
		});
		bus.addListener((LivingAttackEvent event) -> {
			if (!event.getEntityLiving().level.isClientSide()
					&& !Hooks.allowDamage(event.getEntityLiving(), event.getSource(), event.getAmount())) {
				event.setCanceled(true);
			}
		});
		// The last listener: the amount is what the entity takes.
		bus.addListener(EventPriority.LOWEST, (LivingDamageEvent event) -> {
			if (!event.isCanceled() && !event.getEntityLiving().level.isClientSide()) {
				Hooks.afterDamage(event.getEntityLiving(), event.getSource(), event.getAmount(), event.getAmount(), false);
			}
		});
		bus.addListener(EventPriority.LOWEST, (LivingDeathEvent event) -> {
			if (!event.isCanceled() && !event.getEntityLiving().level.isClientSide()) {
				Hooks.afterDeath(event.getEntityLiving(), event.getSource());
			}
		});
		bus.addListener((PlayerInteractEvent.RightClickItem event) -> {
			InteractionResult result = Hooks.useItem(event.getPlayer(), event.getWorld(), event.getHand());
			if (result != InteractionResult.PASS) {
				event.setCanceled(true);
				event.setCancellationResult(result);
			}
		});
		bus.addListener((PlayerInteractEvent.RightClickBlock event) -> {
			InteractionResult result = Hooks.useBlock(event.getPlayer(), event.getWorld(), event.getHand(), event.getHitVec());
			if (result != InteractionResult.PASS) {
				event.setCanceled(true);
				event.setCancellationResult(result);
			}
		});
		bus.addListener((PlayerInteractEvent.EntityInteract event) -> {
			InteractionResult result = Hooks.useEntity(event.getPlayer(), event.getWorld(), event.getHand(), event.getTarget(), null);
			if (result != InteractionResult.PASS) {
				event.setCanceled(true);
				event.setCancellationResult(result);
			}
		});
		bus.addListener((BlockEvent.BreakEvent event) -> {
			if (event.getWorld() instanceof Level level && !Hooks.beforeBlockBreak(level, event.getPlayer(), event.getPos(),
					event.getState(), level.getBlockEntity(event.getPos()))) {
				event.setCanceled(true);
			}
		});
		bus.addListener((PlayerSleepInBedEvent event) -> {
			Player.BedSleepingProblem problem = Hooks.allowSleeping(event.getPlayer(), event.getPos());
			if (problem != null) {
				event.setResult(problem);
			}
		});
		bus.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
			if (event.getPlayer() instanceof ServerPlayer player) {
				Hooks.playerJoin(player);
			}
		});
		bus.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
			if (event.getPlayer() instanceof ServerPlayer player) {
				Hooks.playerLeave(player);
			}
		});
		bus.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
			if (event.getPlayer() instanceof ServerPlayer player) {
				Hooks.playerChangedLevel(player);
			}
		});
		bus.addListener((RegisterCommandsEvent event) -> WorldEventsCommand.register(event.getDispatcher()));
	}
}
