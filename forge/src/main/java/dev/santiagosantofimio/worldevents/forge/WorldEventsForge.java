package dev.santiagosantofimio.worldevents.forge;

import com.mojang.serialization.MapCodec;
import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.command.WorldEventsCommand;
import dev.santiagosantofimio.worldevents.platform.GameEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Starts the mod on Forge and feeds {@link GameEvents} from Forge's events. */
@Mod(WorldEventsMod.MOD_ID)
public class WorldEventsForge {
	private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
			DeferredRegister.create(ForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS, WorldEventsMod.MOD_ID);

	public WorldEventsForge(FMLJavaModLoadingContext context) {
		WorldEventsMod.init(FMLPaths.CONFIGDIR.get());
		LOOT_MODIFIERS.register("game_events", () -> GameEventsLootModifier.CODEC);
		LOOT_MODIFIERS.register(context.getModBusGroup());
		if (FMLEnvironment.dist == Dist.CLIENT) {
			WorldEventsForgeClient.init(context.getModBusGroup());
		}

		TickEvent.ServerTickEvent.Post.BUS.addListener(event -> GameEvents.serverTick(event.server()));
		TickEvent.LevelTickEvent.Post.BUS.addListener(event -> {
			if (event.level() instanceof ServerLevel level) {
				GameEvents.levelTick(level);
			}
		});
		ServerStoppedEvent.BUS.addListener(event -> GameEvents.serverStopped(event.getServer()));
		// Last, so nothing is tracked that another mod keeps out. It runs just before the entity joins the level.
		EntityJoinLevelEvent.BUS.addListener(Priority.LOWEST, event -> {
			if (event.getLevel() instanceof ServerLevel level) {
				GameEvents.entityLoad(event.getEntity(), level);
			}
		});
		LivingAttackEvent.BUS.addListener(event ->
				isServerSide(event.getEntity()) && !GameEvents.allowDamage(event.getEntity(), event.getSource(), event.getAmount()));
		// Comes just before the health goes down, and only when some of it will. Last, once other mods changed the amount.
		LivingDamageEvent.BUS.addListener(Priority.LOWEST, event -> {
			LivingEntity entity = event.getEntity();
			// As on Fabric: a killing blow only counts as a death.
			if (isServerSide(entity) && entity.getHealth() > event.getAmount()) {
				GameEvents.afterDamage(entity, event.getSource(), event.getAmount(), event.getAmount(), false);
			}
		});
		// The event comes at the start of the death, before the loot; last, because another mod can still cancel it.
		LivingDeathEvent.BUS.addListener(Priority.LOWEST, event -> {
			if (isServerSide(event.getEntity())) {
				GameEvents.afterDeath(event.getEntity(), event.getSource());
			}
		});
		// Anything but PASS settles a click, instead of letting it go on to vanilla.
		PlayerInteractEvent.RightClickItem.BUS.addListener(event -> {
			InteractionResult result = GameEvents.useItem(event.getEntity(), event.getLevel(), event.getHand());
			event.setCancellationResult(result);
			return result != InteractionResult.PASS;
		});
		PlayerInteractEvent.RightClickBlock.BUS.addListener(event -> {
			InteractionResult result = GameEvents.useBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
			event.setCancellationResult(result);
			return result != InteractionResult.PASS;
		});
		PlayerInteractEvent.EntityInteractSpecific.BUS.addListener(event -> {
			InteractionResult result = GameEvents.useEntity(event.getEntity(), event.getLevel(), event.getHand(), event.getTarget());
			event.setCancellationResult(result);
			return result != InteractionResult.PASS;
		});
		BlockEvent.BreakEvent.BUS.addListener(event -> {
			if (event.getLevel() instanceof Level level
					&& !GameEvents.beforeBlockBreak(level, event.getPlayer(), event.getPos(), event.getState(), level.getBlockEntity(event.getPos()))) {
				event.setResult(Result.DENY);
			}
		});
		PlayerSleepInBedEvent.BUS.addListener(event -> {
			var problem = GameEvents.allowSleeping(event.getEntity(), event.getOptionalPos().orElse(null));
			if (problem != null) {
				event.setResult(problem);
			}
		});
		PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				GameEvents.playerJoin(player);
			}
		});
		PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				GameEvents.playerLeave(player);
			}
		});
		PlayerEvent.PlayerChangedDimensionEvent.BUS.addListener(event -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				GameEvents.playerChangeLevel(player);
			}
		});
		RegisterCommandsEvent.BUS.addListener(event -> WorldEventsCommand.register(event.getDispatcher()));
	}

	private static boolean isServerSide(LivingEntity entity) {
		return !entity.level().isClientSide();
	}
}
