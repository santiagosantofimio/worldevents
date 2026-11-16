package dev.santiagosantofimio.worldevents.neoforge;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.item.CreativeTab;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The creative tab, which only exists on the client. */
@Mod(value = WorldEventsMod.MOD_ID, dist = Dist.CLIENT)
public class WorldEventsNeoForgeClient {
	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WorldEventsMod.MOD_ID);

	static {
		TABS.register(CreativeTab.ID.getPath(), () -> CreativeTab.configure(CreativeModeTab.builder()).build());
	}

	public WorldEventsNeoForgeClient(IEventBus modBus) {
		TABS.register(modBus);
	}
}
