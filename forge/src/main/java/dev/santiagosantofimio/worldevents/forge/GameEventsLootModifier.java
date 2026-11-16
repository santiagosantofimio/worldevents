package dev.santiagosantofimio.worldevents.forge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.santiagosantofimio.worldevents.platform.GameEvents;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/** Runs on everything a loot table rolls: this is how {@link GameEvents#MODIFY_DROPS} reaches Forge. */
public class GameEventsLootModifier extends LootModifier {
	public static final MapCodec<GameEventsLootModifier> CODEC =
			RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, GameEventsLootModifier::new));

	public GameEventsLootModifier(LootItemCondition[] conditions) {
		super(conditions);
	}

	@Override
	protected ObjectArrayList<ItemStack> doApply(LootTable table, ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
		GameEvents.modifyDrops(context, generatedLoot);
		return generatedLoot;
	}

	@Override
	public MapCodec<? extends IGlobalLootModifier> codec() {
		return CODEC;
	}
}
