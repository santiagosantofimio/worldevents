package dev.santiagosantofimio.worldevents.fabric;

import dev.santiagosantofimio.worldevents.item.CreativeTab;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

/** The creative tab, which only exists on the client. */
public class WorldEventsFabricClient implements ClientModInitializer {
	public static final ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, CreativeTab.ID);

	@Override
	public void onInitializeClient() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB, CreativeTab.configure(FabricCreativeModeTab.builder()).build());
	}
}
