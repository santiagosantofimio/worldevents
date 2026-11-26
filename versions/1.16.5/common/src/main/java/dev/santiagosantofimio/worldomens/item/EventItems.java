package dev.santiagosantofimio.worldomens.item;

import dev.santiagosantofimio.worldomens.WorldOmensMod;
import dev.santiagosantofimio.worldomens.i18n.Lang;
import dev.santiagosantofimio.worldomens.mob.Creature;
import dev.santiagosantofimio.worldomens.platform.Hooks;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public final class EventItems {
	public static final String DATA_KEY = WorldOmensMod.MOD_ID + "_item";

	private static final Map<String, EventItem> BY_ID = new LinkedHashMap<>();
	private static final Map<EventItem, Food> FOOD = new LinkedHashMap<>();

	private static final Map<Item, Item> SMELTED = Map.of(
			Items.IRON_ORE, Items.IRON_INGOT,
			Items.GOLD_ORE, Items.GOLD_INGOT,
			Items.ANCIENT_DEBRIS, Items.NETHERITE_SCRAP);

	private static final float SCYTHE_LIFESTEAL = 0.15f;

	public static final EventItem METEORITE_FRAGMENT = food(register(new EventItem("meteorite_fragment", Items.IRON_NUGGET, Rarity.UNCOMMON, false,
			stack -> {
			})), SoundEvents.NOTE_BLOCK_CHIME,
			new MobEffectInstance(MobEffects.DIG_SPEED, minutes(3), 1),
			new MobEffectInstance(MobEffects.NIGHT_VISION, minutes(3), 0));

	public static final EventItem LUCKY_NUGGET = food(register(new EventItem("lucky_nugget", Items.GOLD_NUGGET, Rarity.RARE, false,
			stack -> {
			})), SoundEvents.EXPERIENCE_ORB_PICKUP,
			new MobEffectInstance(MobEffects.LUCK, minutes(5), 0),
			new MobEffectInstance(MobEffects.DIG_SPEED, minutes(5), 0));

	public static final EventItem BLOODY_TEAR = register(new EventItem("bloody_tear", Items.GHAST_TEAR, Rarity.EPIC, true, stack -> {
	}));

	public static final EventItem BLOOD_SCYTHE = register(new EventItem("blood_scythe", Items.DIAMOND_SWORD, Rarity.EPIC, false,
			stack -> weapon(stack, 8.0, -2.8)));

	public static final EventItem BOTTLED_LIGHTNING = register(new EventItem("bottled_lightning", Items.HONEY_BOTTLE, Rarity.RARE, true, stack -> {
	}));

	public static final EventItem STARFALL_PICKAXE = register(new EventItem("starfall_pickaxe", Items.DIAMOND_PICKAXE, Rarity.EPIC, false, stack -> {
	}));

	public static final EventItem GOLDEN_SEED = register(new EventItem("golden_seed", Items.GLISTERING_MELON_SLICE, Rarity.RARE, true, stack -> {
	}));

	public static final int WAR_CRY_COOLDOWN_SECONDS = 45;

	public static final EventItem WARLORD_AXE = register(new EventItem("warlord_axe", Items.DIAMOND_AXE, Rarity.EPIC, false,
			stack -> weapon(stack, 9.0, -3.0)));

	public static final EventItem CAPTAINS_CUTLASS = register(new EventItem("captains_cutlass", Items.IRON_SWORD, Rarity.EPIC, false,
			stack -> weapon(stack, 6.0, -2.2)));

	public static final EventItem MAGMA_HEART = food(register(new EventItem("magma_heart", Items.MAGMA_CREAM, Rarity.RARE, false,
			stack -> {
			})), SoundEvents.FIRECHARGE_USE,
			new MobEffectInstance(MobEffects.FIRE_RESISTANCE, minutes(5), 0),
			new MobEffectInstance(MobEffects.DAMAGE_BOOST, minutes(2), 0));

	public static final EventItem COLOSSUS_HAMMER = register(new EventItem("colossus_hammer", Items.NETHERITE_AXE, Rarity.EPIC, false,
			stack -> weapon(stack, 9.0, -3.2)));

	public static final EventItem UNSTABLE_PEARL = register(new EventItem("unstable_pearl", Items.POPPED_CHORUS_FRUIT, Rarity.RARE, true, stack -> {
	}));

	public static final int REVEAL_COOLDOWN_SECONDS = 60;

	public static final EventItem ECLIPSE_SHARD = register(new EventItem("eclipse_shard", Items.PRISMARINE_SHARD, Rarity.EPIC, true, stack -> {
	}));

	public static final EventItem FROSTFANG = register(new EventItem("frostfang", Items.DIAMOND_SWORD, Rarity.EPIC, false,
			stack -> weapon(stack, 7.0, -2.4)));

	public static final EventItem EVENT_SEAL = register(new EventItem("event_seal", Items.NAUTILUS_SHELL, Rarity.UNCOMMON, true, stack -> {
	}));

	public static final String CREATURE_KEY = WorldOmensMod.MOD_ID + "_creature";
	private static final String EGG_NAME_KEY = "item." + WorldOmensMod.MOD_ID + ".spawn_egg";
	private static final String EGG_ID_SUFFIX = "_spawn_egg";

	private EventItems() {
	}

	public record EventItem(String id, Item base, Rarity rarity, boolean glint, Consumer<ItemStack> extra) {
		public String nameKey() {
			return "item." + WorldOmensMod.MOD_ID + "." + id;
		}

		public ItemStack create(String language, int count) {
			ItemStack stack = new ItemStack(base, count);
			describe(stack, Lang.text(language, nameKey()), rarity.color, Lang.text(language, nameKey() + ".lore"));
			CompoundTag tag = stack.getOrCreateTag();
			tag.putInt("CustomModelData", modelNumber(id));
			tag.putString(DATA_KEY, id);
			if (glint) {
				stack.enchant(Enchantments.UNBREAKING, 1);
				stack.hideTooltipPart(ItemStack.TooltipPart.ENCHANTMENTS);
			}
			extra.accept(stack);
			return stack;
		}

		public boolean is(ItemStack stack) {
			return identify(stack) == this;
		}
	}

	private record Food(SoundEvent sound, List<MobEffectInstance> effects) {
	}

	private static EventItem register(EventItem item) {
		BY_ID.put(item.id(), item);
		return item;
	}

	private static EventItem food(EventItem item, SoundEvent sound, MobEffectInstance... effects) {
		FOOD.put(item, new Food(sound, List.of(effects)));
		return item;
	}

	public static EventItem get(String id) {
		return BY_ID.get(id);
	}

	public static Collection<EventItem> all() {
		return Collections.unmodifiableCollection(BY_ID.values());
	}

	public static EventItem identify(ItemStack stack) {
		if (stack == null || !stack.hasTag()) {
			return null;
		}
		return BY_ID.get(stringOrLegacy(stack.getTag(), DATA_KEY, "worldevents_item"));
	}

	public static ItemStack egg(Creature.Kind kind, String language, int count) {
		ItemStack stack = new ItemStack(kind.egg(), count);
		ChatFormatting color = kind.bossBar() != null ? Rarity.EPIC.color : Rarity.UNCOMMON.color;
		describe(stack, eggName(kind, language), color, Lang.text(language, EGG_NAME_KEY + ".lore"));
		CompoundTag tag = stack.getOrCreateTag();
		tag.putInt("CustomModelData", modelNumber(kind.id()));
		tag.putString(CREATURE_KEY, kind.id());
		return stack;
	}

	public static ItemStack sealCost(int count) {
		ItemStack stack = new ItemStack(EVENT_SEAL.base(), count);
		CompoundTag tag = stack.getOrCreateTag();
		tag.putString(DATA_KEY, EVENT_SEAL.id());
		tag.putInt("CustomModelData", modelNumber(EVENT_SEAL.id()));
		return stack;
	}

	public static Creature.Kind creatureOf(ItemStack stack) {
		if (stack == null || !stack.hasTag()) {
			return null;
		}
		return Creature.kind(stringOrLegacy(stack.getTag(), CREATURE_KEY, "worldevents_creature"));
	}

	public static String eggId(Creature.Kind kind) {
		return kind.id() + EGG_ID_SUFFIX;
	}

	public static Creature.Kind eggKind(String id) {
		return id.endsWith(EGG_ID_SUFFIX) ? Creature.kind(id.substring(0, id.length() - EGG_ID_SUFFIX.length())) : null;
	}

	public static String eggName(Creature.Kind kind, String language) {
		return Lang.text(language, EGG_NAME_KEY, Lang.text(language, kind.nameKey()));
	}

	public static ItemStack cosmetic(Item base, String modelId) {
		ItemStack stack = new ItemStack(base);
		stack.getOrCreateTag().putInt("CustomModelData", modelNumber(modelId));
		return stack;
	}

	public static int modelNumber(String id) {
		return Math.floorMod((WorldOmensMod.MOD_ID + ":" + id).hashCode(), 9_000_000) + 1_000_000;
	}

	private static void describe(ItemStack stack, String name, ChatFormatting color, String lore) {
		stack.setHoverName(new TextComponent(name).withStyle(style -> style.withColor(color).withItalic(false)));
		ListTag lines = new ListTag();
		Component line = new TextComponent(lore).withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false));
		lines.add(StringTag.valueOf(Component.Serializer.toJson(line)));
		stack.getOrCreateTagElement("display").put("Lore", lines);
	}

	private static void weapon(ItemStack stack, double damage, double speed) {
		stack.addAttributeModifier(Attributes.ATTACK_DAMAGE,
				new AttributeModifier(BaseIds.DAMAGE, "Weapon modifier", damage, AttributeModifier.Operation.ADDITION), EquipmentSlot.MAINHAND);
		stack.addAttributeModifier(Attributes.ATTACK_SPEED,
				new AttributeModifier(BaseIds.SPEED, "Weapon modifier", speed, AttributeModifier.Operation.ADDITION), EquipmentSlot.MAINHAND);
	}

	private static final class BaseIds extends Item {
		private static final UUID DAMAGE = BASE_ATTACK_DAMAGE_UUID;
		private static final UUID SPEED = BASE_ATTACK_SPEED_UUID;

		private BaseIds() {
			super(new Item.Properties());
		}
	}

	private static int minutes(int minutes) {
		return minutes * 60 * 20;
	}

	public static void registerHooks() {
		Hooks.USE_ITEM.register((player, world, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			EventItem item = identify(stack);
			Food food = item != null ? FOOD.get(item) : null;
			if (food == null || !(world instanceof ServerLevel level)) {
				return food != null ? InteractionResult.SUCCESS : InteractionResult.PASS;
			}
			for (MobEffectInstance effect : food.effects()) {
				player.addEffect(new MobEffectInstance(effect));
			}
			level.playSound(null, player.getX(), player.getY(), player.getZ(), food.sound(), SoundSource.PLAYERS, 1.0f, 1.0f);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.5f, 1.0f);
			level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack), player.getX(), player.getEyeY() - 0.2, player.getZ(),
					8, 0.2, 0.1, 0.2, 0.05);
			if (!player.abilities.instabuild) {
				stack.shrink(1);
			}
			return InteractionResult.SUCCESS;
		});

		Hooks.USE_BLOCK.register((player, world, hand, hit) -> {
			ItemStack stack = player.getItemInHand(hand);
			Creature.Kind kind = creatureOf(stack);
			if (kind == null || !(player instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel level)) {
				return InteractionResult.PASS;
			}
			BlockPos clicked = hit.getBlockPos();
			BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(hit.getDirection());
			if (Creature.summon(kind, level, pos, Lang.language(serverPlayer), serverPlayer) == null) {
				return InteractionResult.FAIL;
			}
			if (!player.abilities.instabuild) {
				stack.shrink(1);
			}
			return InteractionResult.SUCCESS;
		});
		Hooks.USE_ITEM.register((player, world, hand) ->
				creatureOf(player.getItemInHand(hand)) != null ? InteractionResult.FAIL : InteractionResult.PASS);
		Hooks.USE_ENTITY.register((player, world, hand, entity, hit) ->
				creatureOf(player.getItemInHand(hand)) != null ? InteractionResult.FAIL : InteractionResult.PASS);

		Hooks.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (damageTaken > 0 && source.getDirectEntity() instanceof Player player && source.getEntity() == player
					&& BLOOD_SCYTHE.is(player.getMainHandItem())) {
				player.heal(damageTaken * SCYTHE_LIFESTEAL);
			}
		});

		Hooks.MODIFY_DROPS.register((table, context, drops) -> {
			ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
			if (tool == null || context.getParamOrNull(LootContextParams.BLOCK_STATE) == null || !STARFALL_PICKAXE.is(tool)) {
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
			Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
			if (smelted && origin != null) {
				context.getLevel().sendParticles(ParticleTypes.FLAME, origin.x, origin.y, origin.z, 8, 0.3, 0.3, 0.3, 0.01);
			}
		});
	}

	private static String stringOrLegacy(CompoundTag tag, String key, String legacyKey) {
		String value = tag.getString(key);
		return value.isEmpty() ? tag.getString(legacyKey) : value;
	}
}
