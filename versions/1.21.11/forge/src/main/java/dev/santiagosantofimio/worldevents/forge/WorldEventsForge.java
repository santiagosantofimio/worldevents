package dev.santiagosantofimio.worldevents.forge;

import com.mojang.serialization.MapCodec;
import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.command.WorldEventsCommand;
import dev.santiagosantofimio.worldevents.platform.Hooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.loot.IGlobalLootModifier;
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
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Forge entry point: sets the mod up and forwards Forge events to {@link Hooks}. */
@Mod(WorldEventsMod.MOD_ID)
public final class WorldEventsForge {
	private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
			DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, WorldEventsMod.MOD_ID);

	static {
		LOOT_MODIFIERS.register("drops", () -> HookLootModifier.CODEC);
	}

	public WorldEventsForge(FMLJavaModLoadingContext context) {
		LOOT_MODIFIERS.register(context.getModBusGroup());
		if (FMLEnvironment.dist == Dist.CLIENT) {
			WorldEventsForgeClient.register(context.getModBusGroup());
		}
		WorldEventsMod.init();

		TickEvent.ServerTickEvent.Post.BUS.addListener(event -> Hooks.endServerTick(event.server()));
		TickEvent.LevelTickEvent.Post.BUS.addListener(event -> {
			if (event.level() instanceof ServerLevel level) {
				Hooks.endLevelTick(level);
			}
		});
		ServerStoppedEvent.BUS.addListener(event -> Hooks.serverStopped(event.getServer()));
		EntityJoinLevelEvent.BUS.addListener(Priority.LOWEST, event -> {
			if (event.getLevel() instanceof ServerLevel level) {
				Hooks.entityLoad(event.getEntity(), level);
			}
			return false;
		});
		LivingAttackEvent.BUS.addListener(event -> !event.getEntity().level().isClientSide()
				&& !Hooks.allowDamage(event.getEntity(), event.getSource(), event.getAmount()));
		// The last listener: the amount is what the entity takes.
		LivingDamageEvent.BUS.addListener(Priority.LOWEST, event -> {
			if (!event.getEntity().level().isClientSide()) {
				Hooks.afterDamage(event.getEntity(), event.getSource(), event.getAmount(), event.getAmount(), false);
			}
			return false;
		});
		LivingDeathEvent.BUS.addListener(Priority.LOWEST, event -> {
			if (!event.getEntity().level().isClientSide()) {
				Hooks.afterDeath(event.getEntity(), event.getSource());
			}
			return false;
		});
		PlayerInteractEvent.RightClickItem.BUS.addListener(event -> {
			InteractionResult result = Hooks.useItem(event.getEntity(), event.getLevel(), event.getHand());
			if (result == InteractionResult.PASS) {
				return false;
			}
			event.setCancellationResult(result);
			return true;
		});
		PlayerInteractEvent.RightClickBlock.BUS.addListener(event -> {
			InteractionResult result = Hooks.useBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
			if (result == InteractionResult.PASS) {
				return false;
			}
			event.setCancellationResult(result);
			return true;
		});
		PlayerInteractEvent.EntityInteract.BUS.addListener(event -> {
			InteractionResult result = Hooks.useEntity(event.getEntity(), event.getLevel(), event.getHand(), event.getTarget(), null);
			if (result == InteractionResult.PASS) {
				return false;
			}
			event.setCancellationResult(result);
			return true;
		});
		BlockEvent.BreakEvent.BUS.addListener(event -> event.getLevel() instanceof Level level
				&& !Hooks.beforeBlockBreak(level, event.getPlayer(), event.getPos(), event.getState(), level.getBlockEntity(event.getPos())));
		PlayerSleepInBedEvent.BUS.addListener(event -> {
			Player.BedSleepingProblem problem = Hooks.allowSleeping(event.getEntity(), event.getPos());
			if (problem != null) {
				event.setResult(problem);
			}
		});
		PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				Hooks.playerJoin(player);
			}
		});
		PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				Hooks.playerLeave(player);
			}
		});
		PlayerEvent.PlayerChangedDimensionEvent.BUS.addListener(event -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				Hooks.playerChangedLevel(player);
			}
		});
		RegisterCommandsEvent.BUS.addListener(event -> WorldEventsCommand.register(event.getDispatcher()));
	}
}
