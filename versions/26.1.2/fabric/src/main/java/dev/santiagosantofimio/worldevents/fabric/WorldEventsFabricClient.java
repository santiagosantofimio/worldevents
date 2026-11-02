package dev.santiagosantofimio.worldevents.fabric;

import dev.santiagosantofimio.worldevents.WorldEventsMod;

import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.mob.Creature;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

/**
 * Only for games that have the mod on the client too (singleplayer, or players who install it): a creative tab with
 * the special items and the creature eggs. Vanilla clients never see it; on a server, the same items and eggs come
 * from {@code /worldevents give}, and creatures from {@code /worldevents summon}.
 */
public class WorldEventsFabricClient implements ClientModInitializer {
	public static final ResourceKey<CreativeModeTab> TAB =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(WorldEventsMod.MOD_ID, "items"));

	@Override
	public void onInitializeClient() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB, FabricCreativeModeTab.builder()
				.title(Component.translatableWithFallback("itemGroup." + WorldEventsMod.MOD_ID, "World Events"))
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

	/** The game's language, for the items' fallback names. */
	private static String language() {
		return Lang.resolveLanguage(Minecraft.getInstance().getLanguageManager().getSelected());
	}
}
