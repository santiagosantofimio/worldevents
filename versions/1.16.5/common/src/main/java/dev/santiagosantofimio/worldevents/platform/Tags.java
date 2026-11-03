package dev.santiagosantofimio.worldevents.platform;

import java.util.Set;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ores and bosses. 1.16 has no shared {@code c:} tags, so these are vanilla's own ores and bosses, plus the ores the
 * loader's own tag lists ({@code forge:ores} on Forge, {@code c:ores} on Fabric if a mod fills it).
 */
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
