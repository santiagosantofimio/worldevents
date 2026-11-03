package dev.santiagosantofimio.worldevents.fabric;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.mob.Creature;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

/**
 * Only for games that have the mod on the client too (singleplayer, or players who install it): a creative tab with
 * the special items and the creature eggs. Vanilla clients never see it; on a server, the same items and eggs come
 * from {@code /worldevents give}, and creatures from {@code /worldevents summon}.
 */
public class WorldEventsFabricClient implements ClientModInitializer {
	public static CreativeModeTab tab;

	@Override
	public void onInitializeClient() {
		tab = FabricItemGroupBuilder.create(new ResourceLocation(WorldEventsMod.MOD_ID, "items"))
				.icon(() -> EventItems.BLOOD_SCYTHE.create(language(), 1))
				.appendItems(stacks -> {
					String language = language();
					for (EventItems.EventItem item : EventItems.all()) {
						stacks.add(item.create(language, 1));
					}
					for (Creature.Kind kind : Creature.kinds()) {
						stacks.add(EventItems.egg(kind, language, 1));
					}
				})
				.build();
	}

	/** The game's language, for the items' names. */
	private static String language() {
		return Lang.resolveLanguage(Minecraft.getInstance().getLanguageManager().getSelected().getCode());
	}
}
