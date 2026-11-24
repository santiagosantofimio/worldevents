package dev.santiagosantofimio.worldevents.forge;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.mob.Creature;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;

/**
 * Only for games that have the mod on the client too (singleplayer, or players who install it): a creative tab with
 * the special items and the creature eggs. Vanilla clients never see it.
 */
final class WorldEventsForgeClient {
	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WorldEventsMod.MOD_ID);

	private WorldEventsForgeClient() {
	}

	static void register(BusGroup modBus) {
		TABS.register("items", () -> CreativeModeTab.builder()
				.title(Component.translatableWithFallback("itemGroup." + WorldEventsMod.MOD_ID, "World Omens"))
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

	/** The game's language, for the items' fallback names. */
	private static String language() {
		return Lang.resolveLanguage(Minecraft.getInstance().getLanguageManager().getSelected());
	}
}
