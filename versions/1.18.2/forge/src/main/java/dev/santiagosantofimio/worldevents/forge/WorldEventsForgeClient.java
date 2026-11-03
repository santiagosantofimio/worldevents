package dev.santiagosantofimio.worldevents.forge;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.mob.Creature;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Only for games that have the mod on the client too (singleplayer, or players who install it): a creative tab with
 * the special items and the creature eggs. Vanilla clients never see it.
 */
final class WorldEventsForgeClient {
	private WorldEventsForgeClient() {
	}

	static void register() {
		new CreativeModeTab(WorldEventsMod.MOD_ID + ".items") {
			@Override
			public ItemStack makeIcon() {
				return EventItems.BLOOD_SCYTHE.create(language(), 1);
			}

			@Override
			public void fillItemList(NonNullList<ItemStack> stacks) {
				String language = language();
				for (EventItems.EventItem item : EventItems.all()) {
					stacks.add(item.create(language, 1));
				}
				for (Creature.Kind kind : Creature.kinds()) {
					stacks.add(EventItems.egg(kind, language, 1));
				}
			}
		};
	}

	/** The game's language, for the items' names. */
	private static String language() {
		return Lang.resolveLanguage(Minecraft.getInstance().getLanguageManager().getSelected().getCode());
	}
}
