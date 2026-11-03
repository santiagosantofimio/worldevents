package dev.santiagosantofimio.worldevents.fabric.mixin;

import dev.santiagosantofimio.worldevents.platform.Hooks;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every loot roll goes through {@link Hooks#MODIFY_DROPS}, like Fabric API's loot table drops event in later
 * versions. Only the outermost table of a roll: tables that a table rolls inside it are part of its drops.
 */
@Mixin(LootTable.class)
public abstract class LootTableMixin {
	@Unique
	private static final ThreadLocal<int[]> worldevents$depth = ThreadLocal.withInitial(() -> new int[1]);

	@Inject(method = "getRandomItemsRaw", at = @At("HEAD"), cancellable = true)
	private void worldevents$modifyDrops(LootContext context, Consumer<ItemStack> output, CallbackInfo ci) {
		int[] depth = worldevents$depth.get();
		if (depth[0] > 0) {
			return;
		}
		List<ItemStack> drops = new ArrayList<>();
		depth[0]++;
		try {
			((LootTable) (Object) this).getRandomItemsRaw(context, drops::add);
		} finally {
			depth[0]--;
		}
		Hooks.modifyDrops(context, drops);
		drops.forEach(output);
		ci.cancel();
	}
}
