package dev.santiagosantofimio.worldomens.forge;

import dev.santiagosantofimio.worldomens.WorldOmensMod;
import dev.santiagosantofimio.worldomens.i18n.Lang;
import dev.santiagosantofimio.worldomens.item.EventItems;
import dev.santiagosantofimio.worldomens.mob.Creature;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;

final class WorldOmensForgeClient {
	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WorldOmensMod.MOD_ID);

	private WorldOmensForgeClient() {
	}

	static void register(BusGroup modBus) {
		TABS.register("items", () -> CreativeModeTab.builder()
				.title(Component.translatableWithFallback("itemGroup." + WorldOmensMod.MOD_ID, "World Omens"))
				.icon(() -> EventItems.BLOOD_SCYTHE.create(language(), 1))
				.displayItems((parameters, output) -> {
					String language = language();
					for (EventItems.EventItem item : EventItems.all()) {
						output.accept(item.create(language, 1));
					}
					for (Creature.Kind kind : Creature.kinds()) {
						output.accept(EventItems.egg(kind, language, 1));
					}
				})
				.build());
		TABS.register(modBus);
	}

	private static String language() {
		return Lang.resolveLanguage(Minecraft.getInstance().getLanguageManager().getSelected());
	}
}
