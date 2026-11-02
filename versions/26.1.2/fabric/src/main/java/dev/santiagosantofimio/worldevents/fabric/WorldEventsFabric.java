package dev.santiagosantofimio.worldevents.fabric;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.command.WorldEventsCommand;
import dev.santiagosantofimio.worldevents.platform.Hooks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

/** Fabric entry point: sets the mod up and forwards Fabric API events to {@link Hooks}. */
public class WorldEventsFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		WorldEventsMod.init();

		ServerTickEvents.END_SERVER_TICK.register(Hooks::endServerTick);
		ServerTickEvents.END_LEVEL_TICK.register(Hooks::endLevelTick);
		ServerLifecycleEvents.SERVER_STOPPED.register(Hooks::serverStopped);
		ServerEntityEvents.ENTITY_LOAD.register(Hooks::entityLoad);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(Hooks::allowDamage);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(Hooks::afterDamage);
		ServerLivingEntityEvents.AFTER_DEATH.register(Hooks::afterDeath);
		UseItemCallback.EVENT.register(Hooks::useItem);
		UseBlockCallback.EVENT.register(Hooks::useBlock);
		UseEntityCallback.EVENT.register(Hooks::useEntity);
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> Hooks.modifyDrops(context, drops));
		PlayerBlockBreakEvents.BEFORE.register(Hooks::beforeBlockBreak);
		EntitySleepEvents.ALLOW_SLEEPING.register(Hooks::allowSleeping);
		ServerPlayerEvents.JOIN.register(Hooks::playerJoin);
		ServerPlayerEvents.LEAVE.register(Hooks::playerLeave);
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) ->
				Hooks.playerChangedLevel(player));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				WorldEventsCommand.register(dispatcher));
	}
}
