package dev.santiagosantofimio.worldevents.neoforge;

import com.mojang.serialization.MapCodec;
import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.command.WorldEventsCommand;
import dev.santiagosantofimio.worldevents.platform.GameEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
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
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Starts the mod on NeoForge and feeds {@link GameEvents} from NeoForge's events. */
@Mod(WorldEventsMod.MOD_ID)
public class WorldEventsNeoForge {
	private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
			DeferredRegister.create(NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS, WorldEventsMod.MOD_ID);

	public WorldEventsNeoForge(IEventBus modBus) {
		WorldEventsMod.init(FMLPaths.CONFIGDIR.get());
		LOOT_MODIFIERS.register("game_events", () -> GameEventsLootModifier.CODEC);
		LOOT_MODIFIERS.register(modBus);

		IEventBus bus = NeoForge.EVENT_BUS;
		bus.addListener((ServerTickEvent.Post event) -> GameEvents.serverTick(event.getServer()));
		bus.addListener((LevelTickEvent.Post event) -> {
			if (event.getLevel() instanceof ServerLevel level) {
				GameEvents.levelTick(level);
			}
		});
		bus.addListener((ServerStoppedEvent event) -> GameEvents.serverStopped(event.getServer()));
		// Last, so nothing is tracked that another mod keeps out. It runs just before the entity joins the level.
		bus.addListener(EventPriority.LOWEST, (EntityJoinLevelEvent event) -> {
			if (event.getLevel() instanceof ServerLevel level) {
				GameEvents.entityLoad(event.getEntity(), level);
			}
		});
		bus.addListener((LivingIncomingDamageEvent event) -> {
			if (isServerSide(event.getEntity()) && !GameEvents.allowDamage(event.getEntity(), event.getSource(), event.getAmount())) {
				event.setCanceled(true);
			}
		});
		bus.addListener((LivingDamageEvent.Post event) -> {
			LivingEntity entity = event.getEntity();
			// As on Fabric: a killing blow only counts as a death.
			if (isServerSide(entity) && !entity.isDeadOrDying()) {
				GameEvents.afterDamage(entity, event.getSource(), event.getOriginalDamage(), event.getInflictedDamage(), event.getBlockedDamage() > 0.0f);
			}
		});
		// The event comes at the start of the death, before the loot; last, because another mod can still cancel it.
		bus.addListener(EventPriority.LOWEST, (LivingDeathEvent event) -> {
			if (isServerSide(event.getEntity())) {
				GameEvents.afterDeath(event.getEntity(), event.getSource());
			}
		});
		// Anything but PASS settles a click, instead of letting it go on to vanilla.
		bus.addListener((PlayerInteractEvent.RightClickItem event) -> {
			InteractionResult result = GameEvents.useItem(event.getEntity(), event.getLevel(), event.getHand());
			if (result != InteractionResult.PASS) {
				event.setCancellationResult(result);
				event.setCanceled(true);
			}
		});
		bus.addListener((PlayerInteractEvent.RightClickBlock event) -> {
			InteractionResult result = GameEvents.useBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
			if (result != InteractionResult.PASS) {
				event.setCancellationResult(result);
				event.setCanceled(true);
			}
		});
		bus.addListener((PlayerInteractEvent.EntityInteract event) -> {
			InteractionResult result = GameEvents.useEntity(event.getEntity(), event.getLevel(), event.getHand(), event.getTarget());
			if (result != InteractionResult.PASS) {
				event.setCancellationResult(result);
				event.setCanceled(true);
			}
		});
		bus.addListener((BreakBlockEvent event) -> {
			if (event.getLevel() instanceof Level level
					&& !GameEvents.beforeBlockBreak(level, event.getPlayer(), event.getPos(), event.getState(), level.getBlockEntity(event.getPos()))) {
				event.setCanceled(true);
			}
		});
		bus.addListener((CanPlayerSleepEvent event) -> {
			var problem = GameEvents.allowSleeping(event.getEntity(), event.getPos());
			if (problem != null) {
				event.setProblem(problem);
			}
		});
		bus.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				GameEvents.playerJoin(player);
			}
		});
		bus.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				GameEvents.playerLeave(player);
			}
		});
		bus.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				GameEvents.playerChangeLevel(player);
			}
		});
		bus.addListener((RegisterCommandsEvent event) -> WorldEventsCommand.register(event.getDispatcher()));
	}

	private static boolean isServerSide(LivingEntity entity) {
		return !entity.level().isClientSide();
	}
}
