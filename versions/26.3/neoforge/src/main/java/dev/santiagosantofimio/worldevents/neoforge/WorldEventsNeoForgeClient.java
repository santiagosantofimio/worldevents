package dev.santiagosantofimio.worldevents.neoforge;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.mob.Creature;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Only for games that have the mod on the client too (singleplayer, or players who install it): a creative tab with
 * the special items and the creature eggs. Vanilla clients never see it.
 */
@Mod(value = WorldEventsMod.MOD_ID, dist = Dist.CLIENT)
public class WorldEventsNeoForgeClient {
	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WorldEventsMod.MOD_ID);

	static {
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
	}

	public WorldEventsNeoForgeClient(IEventBus modBus) {
		TABS.register(modBus);
	}

	/** The game's language, for the items' fallback names. */
	private static String language() {
		return Lang.resolveLanguage(Minecraft.getInstance().getLanguageManager().getSelected());
	}
}
