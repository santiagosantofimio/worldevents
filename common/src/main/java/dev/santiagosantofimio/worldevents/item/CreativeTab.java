package dev.santiagosantofimio.worldevents.item;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;

/**
 * A creative tab with the special items and the creature eggs, registered only on the client by each mod loader. A
 * client without the mod never sees it; there, the same items and eggs come from {@code /worldevents give}.
 */
public final class CreativeTab {
	public static final Identifier ID = Identifier.fromNamespaceAndPath(WorldEventsMod.MOD_ID, "items");

	private CreativeTab() {
	}

	public static CreativeModeTab.Builder configure(CreativeModeTab.Builder builder) {
		return builder
				.title(Component.translatableWithFallback("itemGroup." + WorldEventsMod.MOD_ID, "World Omens"))
				.icon(() -> EventItems.BLOOD_SCYTHE.create(language(), 1))
				.displayItems((parameters, output) -> EventItems.forEachCreativeStack(language(), output::accept));
	}

	/** The game's language, for the items' fallback names. */
	private static String language() {
		return Lang.resolveLanguage(Minecraft.getInstance().getLanguageManager().getSelected());
	}
}
