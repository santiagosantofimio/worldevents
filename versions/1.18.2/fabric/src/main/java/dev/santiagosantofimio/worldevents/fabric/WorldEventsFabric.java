package dev.santiagosantofimio.worldevents.fabric;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.command.WorldEventsCommand;
import dev.santiagosantofimio.worldevents.platform.Hooks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric entry point: sets the mod up and forwards Fabric API events to {@link Hooks}. Fabric API for 1.18 has no
 * events for damage, death or loot, so the mixins in {@code fabric.mixin} call those hooks.
 */
public class WorldEventsFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		WorldEventsMod.init();

		ServerTickEvents.END_SERVER_TICK.register(Hooks::endServerTick);
		ServerTickEvents.END_WORLD_TICK.register(Hooks::endLevelTick);
		ServerLifecycleEvents.SERVER_STOPPED.register(Hooks::serverStopped);
		ServerEntityEvents.ENTITY_LOAD.register(Hooks::entityLoad);
		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			InteractionResult result = Hooks.useItem(player, world, hand);
			return result == InteractionResult.PASS ? InteractionResultHolder.pass(stack) : new InteractionResultHolder<>(result, stack);
		});
		UseBlockCallback.EVENT.register(Hooks::useBlock);
		UseEntityCallback.EVENT.register(Hooks::useEntity);
		PlayerBlockBreakEvents.BEFORE.register(Hooks::beforeBlockBreak);
		EntitySleepEvents.ALLOW_SLEEPING.register(Hooks::allowSleeping);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Hooks.playerJoin(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			Hooks.playerLeave(handler.player);
			FabricPlatform.forget(handler.player);
		});
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
				Hooks.playerChangedLevel(player));
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> WorldEventsCommand.register(dispatcher));
	}
}
