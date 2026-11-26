package dev.santiagosantofimio.worldomens.platform;

import java.util.Set;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class Tags {
	private static final Set<Block> VANILLA_ORES = Set.of(Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.GOLD_ORE, Blocks.DIAMOND_ORE,
			Blocks.EMERALD_ORE, Blocks.LAPIS_ORE, Blocks.REDSTONE_ORE, Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE,
			Blocks.ANCIENT_DEBRIS);

	private Tags() {
	}

	public static boolean isOre(BlockState state) {
		return VANILLA_ORES.contains(state.getBlock()) || Platform.INSTANCE.isOre(state);
	}

	public static boolean isBoss(EntityType<?> type) {
		return type == EntityType.ENDER_DRAGON || type == EntityType.WITHER;
	}
}
