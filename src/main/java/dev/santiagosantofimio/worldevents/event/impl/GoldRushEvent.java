package dev.santiagosantofimio.worldevents.event.impl;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldevents.config.EventSettings;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.WorldEvent;
import dev.santiagosantofimio.worldevents.util.Drops;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** For a few minutes, ores mined by players drop more. */
public final class GoldRushEvent implements WorldEvent {
	public static final String ID = "gold_rush";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public JsonObject defaultSettings() {
		// At most once every 4 in-game days; lasts 5 minutes.
		JsonObject json = EventSettings.defaults(true, 10, 4800, 300);
		json.addProperty("dropMultiplier", 2.0);
		return json;
	}

	@Override
	public BossEvent.BossBarColor barColor() {
		return BossEvent.BossBarColor.YELLOW;
	}

	@Override
	public ChatFormatting textColor() {
		return ChatFormatting.GOLD;
	}

	@Override
	public Holder<SoundEvent> startSound() {
		return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.PLAYER_LEVELUP);
	}

	@Override
	public void registerHooks() {
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
			if (state == null || !state.is(ConventionalBlockTags.ORES)) {
				return;
			}
			if (!(context.getOptional(LootContextParams.THIS_ENTITY) instanceof Player) || !EventManager.isActive(context.getLevel(), this)) {
				return;
			}
			// Silk touch (or ores that always drop themselves, like ancient debris) would just duplicate the block.
			Item blockItem = state.getBlock().asItem();
			for (ItemStack stack : drops) {
				if (stack.is(blockItem)) {
					return;
				}
			}
			Drops.multiply(drops, settings().getDouble("dropMultiplier"), context.getRandom());
		});
	}
}
