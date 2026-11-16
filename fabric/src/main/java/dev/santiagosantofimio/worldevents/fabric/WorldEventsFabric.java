package dev.santiagosantofimio.worldevents.fabric;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.command.WorldEventsCommand;
import dev.santiagosantofimio.worldevents.platform.GameEvents;
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
import net.fabricmc.loader.api.FabricLoader;

/** Starts the mod on Fabric and feeds {@link GameEvents} from Fabric API. */
public class WorldEventsFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		WorldEventsMod.init(FabricLoader.getInstance().getConfigDir());

		ServerTickEvents.END_SERVER_TICK.register(GameEvents::serverTick);
		ServerTickEvents.END_LEVEL_TICK.register(GameEvents::levelTick);
		ServerLifecycleEvents.SERVER_STOPPED.register(GameEvents::serverStopped);
		ServerEntityEvents.ENTITY_LOAD.register(GameEvents::entityLoad);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(GameEvents::allowDamage);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(GameEvents::afterDamage);
		ServerLivingEntityEvents.AFTER_DEATH.register(GameEvents::afterDeath);
		UseItemCallback.EVENT.register(GameEvents::useItem);
		UseBlockCallback.EVENT.register(GameEvents::useBlock);
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> GameEvents.useEntity(player, level, hand, entity));
		PlayerBlockBreakEvents.BEFORE.register(GameEvents::beforeBlockBreak);
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> GameEvents.modifyDrops(context, drops));
		EntitySleepEvents.ALLOW_SLEEPING.register(GameEvents::allowSleeping);
		ServerPlayerEvents.JOIN.register(GameEvents::playerJoin);
		ServerPlayerEvents.LEAVE.register(GameEvents::playerLeave);
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> GameEvents.playerChangeLevel(player));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> WorldEventsCommand.register(dispatcher));
	}
}
