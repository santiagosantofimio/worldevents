package dev.santiagosantofimio.worldevents.item;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.mob.Creature;
import dev.santiagosantofimio.worldevents.platform.Hooks;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * The mod's special items. Each one is a vanilla item with extra components (name, description, rarity, effects),
 * so vanilla clients can join and use them. The optional resource pack gives them their own look through
 * {@code custom_model_data}; without it they look like their base item.
 *
 * <p>Names are translatable with a fallback: clients with the resource pack (or the mod) show them in their own
 * language, the rest see the fallback text in the language of the player who got the item.
 */
public final class EventItems {
	/** Key in an item's custom data that says which special item it is. */
	public static final String DATA_KEY = WorldEventsMod.MOD_ID + "_item";

	private static final Map<String, EventItem> BY_ID = new LinkedHashMap<>();

	/** Mined with the starfall pickaxe, raw ores come out already smelted. */
	private static final Map<Item, Item> SMELTED = Map.of(
			Items.RAW_IRON, Items.IRON_INGOT,
			Items.RAW_GOLD, Items.GOLD_INGOT,
			Items.RAW_COPPER, Items.COPPER_INGOT);

	private static final float SCYTHE_LIFESTEAL = 0.15f;

	public static final EventItem METEORITE_FRAGMENT = register(new EventItem("meteorite_fragment", Items.RAW_IRON, Rarity.UNCOMMON, false,
			stack -> stack.set(DataComponents.CONSUMABLE, consumable(SoundEvents.AMETHYST_BLOCK_CHIME,
					new MobEffectInstance(MobEffects.HASTE, minutes(3), 1),
					new MobEffectInstance(MobEffects.NIGHT_VISION, minutes(3), 0)))));

	public static final EventItem LUCKY_NUGGET = register(new EventItem("lucky_nugget", Items.GOLD_NUGGET, Rarity.RARE, false,
			stack -> stack.set(DataComponents.CONSUMABLE, consumable(SoundEvents.EXPERIENCE_ORB_PICKUP,
					new MobEffectInstance(MobEffects.LUCK, minutes(5), 0),
					new MobEffectInstance(MobEffects.HASTE, minutes(5), 0)))));

	/** Summons a blood moon when used at night; see {@code BloodMoonEvent}. */
	public static final EventItem BLOODY_TEAR = register(new EventItem("bloody_tear", Items.GHAST_TEAR, Rarity.EPIC, true, stack -> {
	}));

	/** Hits harder but slower than a diamond sword, and heals the wielder a little. */
	public static final EventItem BLOOD_SCYTHE = register(new EventItem("blood_scythe", Items.DIAMOND_SWORD, Rarity.EPIC, false,
			stack -> stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
					.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 8.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
					.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.8, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
					.build())));

	/**
	 * Calls a lightning bolt where the player looks; see {@code ThunderstormEvent}. A honey bottle that cannot be
	 * drunk: dragon's breath, the obvious base, would hand out the End's "You Need a Mint" advancement.
	 */
	public static final EventItem BOTTLED_LIGHTNING = register(new EventItem("bottled_lightning", Items.HONEY_BOTTLE, Rarity.RARE, true, stack -> {
		stack.remove(DataComponents.CONSUMABLE);
		stack.remove(DataComponents.FOOD);
	}));

	/** Smelts raw ores as it mines them. */
	public static final EventItem STARFALL_PICKAXE = register(new EventItem("starfall_pickaxe", Items.DIAMOND_PICKAXE, Rarity.EPIC, false, stack -> {
	}));

	/** Grows the crops around the block it is used on; see {@code BlessedHarvestEvent}. */
	public static final EventItem GOLDEN_SEED = register(new EventItem("golden_seed", Items.GLISTERING_MELON_SLICE, Rarity.RARE, true, stack -> {
	}));

	/** Cooldown group of the war cry, so the cooldown only shows on this axe and not on every diamond axe. */
	public static final Identifier WAR_CRY = Identifier.fromNamespaceAndPath(WorldEventsMod.MOD_ID, "war_cry");
	public static final int WAR_CRY_COOLDOWN_SECONDS = 45;

	/** Hits harder than a diamond axe and lets out a war cry; see {@code HordeEvent}. */
	public static final EventItem WARLORD_AXE = register(new EventItem("warlord_axe", Items.DIAMOND_AXE, Rarity.EPIC, false, stack -> {
		stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 9.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.build());
		stack.set(DataComponents.USE_COOLDOWN, abilityCooldown(WAR_CRY));
	}));

	/** Cooldown group of the cutlass's lunge. */
	public static final Identifier LUNGE = Identifier.fromNamespaceAndPath(WorldEventsMod.MOD_ID, "lunge");

	/** Quick and light, and lunges forward on right-click; see {@code TreasureHuntEvent}. */
	public static final EventItem CAPTAINS_CUTLASS = register(new EventItem("captains_cutlass", Items.IRON_SWORD, Rarity.EPIC, false, stack -> {
		stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 6.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.build());
		stack.set(DataComponents.USE_COOLDOWN, abilityCooldown(LUNGE));
	}));

	/** Fire resistance and strength; left by monsters killed during an eruption and by the colossus. */
	public static final EventItem MAGMA_HEART = register(new EventItem("magma_heart", Items.MAGMA_CREAM, Rarity.RARE, false,
			stack -> stack.set(DataComponents.CONSUMABLE, consumable(SoundEvents.FIRECHARGE_USE,
					new MobEffectInstance(MobEffects.FIRE_RESISTANCE, minutes(5), 0),
					new MobEffectInstance(MobEffects.STRENGTH, minutes(2), 0)))));

	/** A heavier mace whose hits set the target on fire; see {@code EruptionEvent}. */
	public static final EventItem COLOSSUS_HAMMER = register(new EventItem("colossus_hammer", Items.MACE, Rarity.EPIC, false,
			stack -> stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
					.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 9.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
					.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3.2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
					.build())));

	/** Moves the player forward in a blink; see {@code VoidStormEvent}. */
	public static final EventItem UNSTABLE_PEARL = register(new EventItem("unstable_pearl", Items.POPPED_CHORUS_FRUIT, Rarity.RARE, true, stack -> {
	}));

	/** Earned by taking part in events and spent at the nomad merchant. Plain currency: it does nothing by itself. */
	public static final EventItem EVENT_SEAL = register(new EventItem("event_seal", Items.PRIZE_POTTERY_SHERD, Rarity.UNCOMMON, true, stack -> {
	}));

	/** Key in a creature spawn egg's custom data that says which creature it summons. */
	public static final String CREATURE_KEY = WorldEventsMod.MOD_ID + "_creature";
	/** Name of every creature egg, with the creature's name in it. */
	private static final String EGG_NAME_KEY = "item." + WorldEventsMod.MOD_ID + ".spawn_egg";
	private static final String EGG_ID_SUFFIX = "_spawn_egg";

	private EventItems() {
	}

	public record EventItem(String id, Item base, Rarity rarity, boolean glint, Consumer<ItemStack> extra) {
		public String nameKey() {
			return "item." + WorldEventsMod.MOD_ID + "." + id;
		}

		/** Creates the item, with its fallback name and description in {@code language}. */
		public ItemStack create(String language, int count) {
			ItemStack stack = new ItemStack(base, count);
			stack.set(DataComponents.ITEM_NAME, Component.translatableWithFallback(nameKey(), Lang.text(language, nameKey())));
			Component lore = Component.translatableWithFallback(nameKey() + ".lore", Lang.text(language, nameKey() + ".lore"))
					.withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false));
			stack.set(DataComponents.LORE, new ItemLore(List.of(lore)));
			stack.set(DataComponents.RARITY, rarity);
			stack.set(DataComponents.CUSTOM_MODEL_DATA, modelData(id));
			CompoundTag tag = new CompoundTag();
			tag.putString(DATA_KEY, id);
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
			if (glint) {
				stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
			}
			extra.accept(stack);
			return stack;
		}

		public boolean is(DataComponentGetter stack) {
			return identify(stack) == this;
		}
	}

	private static EventItem register(EventItem item) {
		BY_ID.put(item.id(), item);
		return item;
	}

	public static EventItem get(String id) {
		return BY_ID.get(id);
	}

	public static Collection<EventItem> all() {
		return Collections.unmodifiableCollection(BY_ID.values());
	}

	/** Which special item {@code stack} is, or {@code null} for an ordinary item. */
	public static EventItem identify(DataComponentGetter stack) {
		if (stack == null) {
			return null;
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null || data.isEmpty()) {
			return null;
		}
		return BY_ID.get(data.copyTag().getStringOr(DATA_KEY, ""));
	}

	/**
	 * A spawn egg for one of the mod's creatures: a vanilla egg, usually its base mob's, which summons the creature
	 * instead. Named in {@code language} for players without the resource pack, like the other items.
	 */
	public static ItemStack egg(Creature.Kind kind, String language, int count) {
		ItemStack stack = new ItemStack(kind.egg(), count);
		Component creature = Component.translatableWithFallback(kind.nameKey(), Lang.text(language, kind.nameKey()));
		// The fallback is the pattern itself: the client fills in the creature's name.
		stack.set(DataComponents.ITEM_NAME, Component.translatableWithFallback(EGG_NAME_KEY, Lang.text(language, EGG_NAME_KEY), creature));
		Component lore = Component.translatableWithFallback(EGG_NAME_KEY + ".lore", Lang.text(language, EGG_NAME_KEY + ".lore"))
				.withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false));
		stack.set(DataComponents.LORE, new ItemLore(List.of(lore)));
		stack.set(DataComponents.RARITY, kind.bossBar() != null ? Rarity.EPIC : Rarity.UNCOMMON);
		stack.set(DataComponents.CUSTOM_MODEL_DATA, modelData(kind.id()));
		CompoundTag tag = new CompoundTag();
		tag.putString(CREATURE_KEY, kind.id());
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return stack;
	}

	/** A price in event seals, for merchants. Only real seals pay it, not plain pottery sherds. */
	public static ItemCost sealCost(int count) {
		CompoundTag tag = new CompoundTag();
		tag.putString(DATA_KEY, EVENT_SEAL.id());
		return new ItemCost(EVENT_SEAL.base().builtInRegistryHolder(), count, DataComponentExactPredicate.builder()
				.expect(DataComponents.CUSTOM_DATA, CustomData.of(tag))
				// Not needed to match it: it makes the merchant's screen show the seal's own icon.
				.expect(DataComponents.CUSTOM_MODEL_DATA, modelData(EVENT_SEAL.id()))
				.build());
	}

	/** The creature a spawn egg summons, or {@code null} for any other item. */
	public static Creature.Kind creatureOf(DataComponentGetter stack) {
		CustomData data = stack == null ? null : stack.get(DataComponents.CUSTOM_DATA);
		if (data == null || data.isEmpty()) {
			return null;
		}
		return Creature.kind(data.copyTag().getStringOr(CREATURE_KEY, ""));
	}

	/** The egg's id for {@code /worldevents give}: {@code <creature>_spawn_egg}. */
	public static String eggId(Creature.Kind kind) {
		return kind.id() + EGG_ID_SUFFIX;
	}

	/** The creature whose egg has the id {@code id}, or {@code null}. */
	public static Creature.Kind eggKind(String id) {
		return id.endsWith(EGG_ID_SUFFIX) ? Creature.kind(id.substring(0, id.length() - EGG_ID_SUFFIX.length())) : null;
	}

	/** The egg's name in {@code language}, for messages. */
	public static String eggName(Creature.Kind kind, String language) {
		return Lang.text(language, EGG_NAME_KEY, Lang.text(language, kind.nameKey()));
	}

	/**
	 * A plain vanilla item that only exists to be worn or held by an event mob. With the resource pack it shows a
	 * custom 3D model; without it, the base item.
	 */
	public static ItemStack cosmetic(Item base, String modelId) {
		ItemStack stack = new ItemStack(base);
		stack.set(DataComponents.CUSTOM_MODEL_DATA, modelData(modelId));
		return stack;
	}

	/**
	 * Only names a cooldown group, so a right-click ability's cooldown shows on that item alone. Its own length is one
	 * tick: vanilla applies it after the item's other uses too (stripping a log), which must not use up the ability.
	 */
	private static UseCooldown abilityCooldown(Identifier group) {
		return new UseCooldown(0.05f, Optional.of(group));
	}

	private static CustomModelData modelData(String id) {
		return new CustomModelData(List.of(), List.of(), List.of(WorldEventsMod.MOD_ID + ":" + id), List.of());
	}

	private static int minutes(int minutes) {
		return minutes * 60 * 20;
	}

	private static Consumable consumable(SoundEvent sound, MobEffectInstance... effects) {
		return Consumable.builder()
				.consumeSeconds(1.0f)
				.animation(ItemUseAnimation.EAT)
				.sound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound))
				.hasConsumeParticles(true)
				.onConsume(new ApplyStatusEffectsConsumeEffect(List.of(effects)))
				.build();
	}

	public static void registerHooks() {
		// Creature eggs: the creature comes out where the egg is used on a block, as with a vanilla egg.
		Hooks.USE_BLOCK.register((player, world, hand, hit) -> {
			ItemStack stack = player.getItemInHand(hand);
			Creature.Kind kind = creatureOf(stack);
			if (kind == null || !(player instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel level)) {
				return InteractionResult.PASS;
			}
			BlockPos clicked = hit.getBlockPos();
			// Inside a block without collision (tall grass…), otherwise next to the face that was clicked.
			BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(hit.getDirection());
			if (Creature.summon(kind, level, pos, Lang.language(serverPlayer), serverPlayer) == null) {
				return InteractionResult.FAIL;
			}
			stack.consume(1, player);
			return InteractionResult.SUCCESS;
		});
		// Not in the air, on water or on a mob, where the vanilla egg would spawn its base mob (or a baby of it).
		Hooks.USE_ITEM.register((player, world, hand) ->
				creatureOf(player.getItemInHand(hand)) != null ? InteractionResult.FAIL : InteractionResult.PASS);
		Hooks.USE_ENTITY.register((player, world, hand, entity, hit) ->
				creatureOf(player.getItemInHand(hand)) != null ? InteractionResult.FAIL : InteractionResult.PASS);

		// Blood scythe: melee hits heal the wielder for part of the damage dealt.
		Hooks.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (damageTaken > 0 && source.getDirectEntity() instanceof Player player && source.getEntity() == player
					&& BLOOD_SCYTHE.is(player.getMainHandItem())) {
				player.heal(damageTaken * SCYTHE_LIFESTEAL);
			}
		});

		// Starfall pickaxe: raw ores drop as ingots.
		Hooks.MODIFY_DROPS.register((table, context, drops) -> {
			ItemInstance tool = context.getOptionalParameter(LootContextParams.TOOL);
			if (tool == null || context.getOptionalParameter(LootContextParams.BLOCK_STATE) == null || !STARFALL_PICKAXE.is(tool)) {
				return;
			}
			boolean smelted = false;
			for (int i = 0; i < drops.size(); i++) {
				Item ingot = SMELTED.get(drops.get(i).getItem());
				if (ingot != null) {
					drops.set(i, new ItemStack(ingot, drops.get(i).getCount()));
					smelted = true;
				}
			}
			Vec3 origin = context.getOptionalParameter(LootContextParams.ORIGIN);
			if (smelted && origin != null) {
				context.getLevel().sendParticles(ParticleTypes.FLAME, origin.x, origin.y, origin.z, 8, 0.3, 0.3, 0.3, 0.01);
			}
		});
	}
}
