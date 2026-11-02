package dev.santiagosantofimio.worldevents.forge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.santiagosantofimio.worldevents.platform.Hooks;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/** Every loot roll goes through {@link Hooks#MODIFY_DROPS}, like Fabric's loot table drops event. */
public class HookLootModifier extends LootModifier {
	public static final MapCodec<HookLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
			codecStart(instance).apply(instance, HookLootModifier::new));

	public HookLootModifier(LootItemCondition[] conditions) {
		super(conditions);
	}

	@Override
	protected ObjectArrayList<ItemStack> doApply(LootTable table, ObjectArrayList<ItemStack> loot, LootContext context) {
		Hooks.modifyDrops(context, loot);
		return loot;
	}

	@Override
	public MapCodec<? extends IGlobalLootModifier> codec() {
		return CODEC;
	}
}
