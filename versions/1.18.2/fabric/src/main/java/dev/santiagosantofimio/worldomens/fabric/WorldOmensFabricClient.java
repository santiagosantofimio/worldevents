package dev.santiagosantofimio.worldomens.fabric;

import dev.santiagosantofimio.worldomens.WorldOmensMod;
import dev.santiagosantofimio.worldomens.i18n.Lang;
import dev.santiagosantofimio.worldomens.item.EventItems;
import dev.santiagosantofimio.worldomens.mob.Creature;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

public class WorldOmensFabricClient implements ClientModInitializer {
	public static CreativeModeTab tab;

	@Override
	public void onInitializeClient() {
		tab = FabricItemGroupBuilder.create(new ResourceLocation(WorldOmensMod.MOD_ID, "items"))
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

	private static String language() {
		return Lang.resolveLanguage(Minecraft.getInstance().getLanguageManager().getSelected().getCode());
	}
}
