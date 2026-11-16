package dev.santiagosantofimio.worldevents.forge;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.item.CreativeTab;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;

/** The creative tab, which only exists on the client. */
final class WorldEventsForgeClient {
	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WorldEventsMod.MOD_ID);

	private WorldEventsForgeClient() {
	}

	static void init(BusGroup modBus) {
		TABS.register(CreativeTab.ID.getPath(), () -> CreativeTab.configure(CreativeModeTab.builder()).build());
		TABS.register(modBus);
	}
}
