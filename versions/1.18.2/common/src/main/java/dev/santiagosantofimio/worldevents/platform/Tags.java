package dev.santiagosantofimio.worldevents.platform;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

/** The shared {@code c:} tags, which Fabric, NeoForge and Forge all fill in. */
public final class Tags {
	private Tags() {
	}

	public static final TagKey<Block> ORES = TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation("c", "ores"));
	public static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registry.ENTITY_TYPE_REGISTRY, new ResourceLocation("c", "bosses"));
}
