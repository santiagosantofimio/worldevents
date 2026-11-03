package dev.santiagosantofimio.worldevents.forge;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldevents.platform.Hooks;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;

/** Every loot roll goes through {@link Hooks#MODIFY_DROPS}, like Fabric's loot table drops event. */
public class HookLootModifier extends LootModifier {
	public HookLootModifier(LootItemCondition[] conditions) {
		super(conditions);
	}

	@Override
	protected List<ItemStack> doApply(List<ItemStack> loot, LootContext context) {
		Hooks.modifyDrops(context, loot);
		return loot;
	}

	public static class Serializer extends GlobalLootModifierSerializer<HookLootModifier> {
		@Override
		public HookLootModifier read(ResourceLocation location, JsonObject object, LootItemCondition[] conditions) {
			return new HookLootModifier(conditions);
		}

		@Override
		public JsonObject write(HookLootModifier instance) {
			return makeConditions(instance.conditions);
		}
	}
}
