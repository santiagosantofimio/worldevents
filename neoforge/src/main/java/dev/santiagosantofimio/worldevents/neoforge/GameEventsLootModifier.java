package dev.santiagosantofimio.worldevents.neoforge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.santiagosantofimio.worldevents.platform.GameEvents;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/** Runs on everything a loot table rolls: this is how {@link GameEvents#MODIFY_DROPS} reaches NeoForge. */
public class GameEventsLootModifier extends LootModifier {
	public static final MapCodec<GameEventsLootModifier> CODEC =
			RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, GameEventsLootModifier::new));

	public GameEventsLootModifier(Optional<Holder<LootItemCondition>> condition, int priority) {
		super(condition, priority);
	}

	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
		GameEvents.modifyDrops(context, generatedLoot);
		return generatedLoot;
	}

	@Override
	public MapCodec<? extends IGlobalLootModifier> codec() {
		return CODEC;
	}
}
