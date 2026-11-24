package dev.santiagosantofimio.worldomens.neoforge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.santiagosantofimio.worldomens.platform.Hooks;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

/** Every loot roll goes through {@link Hooks#MODIFY_DROPS}, like Fabric's loot table drops event. */
public class HookLootModifier extends LootModifier {
	public static final MapCodec<HookLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
			codecStart(instance).apply(instance, HookLootModifier::new));

	public HookLootModifier(Optional<Holder<LootItemCondition>> condition, int priority) {
		super(condition, priority);
	}

	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
		Hooks.modifyDrops(context, loot);
		return loot;
	}

	@Override
	public MapCodec<? extends LootModifier> codec() {
		return CODEC;
	}
}
