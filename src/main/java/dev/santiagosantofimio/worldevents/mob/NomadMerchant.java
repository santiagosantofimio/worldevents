package dev.santiagosantofimio.worldevents.mob;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.config.ModConfig;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.impl.MerchantCaravanEvent;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.util.Advancements;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * The nomad merchant: a hooded traveller bent under a towering pack of goods, who sells rare wares during a
 * merchant caravan. A wandering trader without AI is the invisible mob under the model, so trading works like with
 * any vanilla merchant. It stays where it set up camp, turns to face nearby players and cannot be hurt.
 */
public final class NomadMerchant {
	public static final Creature.Kind KIND = Creature.kind("nomad_merchant", Items.WANDERING_TRADER_SPAWN_EGG,
					// A summoned merchant never packs up.
					(level, pos, language, summoner) -> spawn(level, pos, language, 0, configuredPrices()))
			.acting(NomadMerchant::act);
	public static final String TAG = KIND.tag();
	public static final String NAME_KEY = KIND.nameKey();
	public static final String LLAMA_TAG = WorldEventsMod.MOD_ID + ".caravan_llama";
	public static final Creature.Part BODY = Creature.register(Creature.Part.of(Items.BARREL, "nomad_merchant", 1.0f).big());
	private static final float PRICE_GROWTH = 0.05f;
	private static final double FACE_PLAYER_RANGE = 8.0;
	/** Trades each merchant has made, to notice new ones. */
	private static final Map<WanderingTrader, Integer> TRADES = new WeakHashMap<>();
	/** The last player each merchant traded with: a player can trade and close the screen within one tick. */
	private static final Map<WanderingTrader, ServerPlayer> LAST_TRADER = new WeakHashMap<>();

	/** Something the merchant sells: what, for how many emeralds, and how many times before it runs out. */
	private record Ware(Function<String, ItemStack> result, int price, int maxUses) {
		static Ware of(EventItems.EventItem item, int count, int price, int maxUses) {
			return new Ware(language -> item.create(language, count), price, maxUses);
		}

		static Ware of(Item item, int count, int price, int maxUses) {
			return new Ware(language -> new ItemStack(item, count), price, maxUses);
		}
	}

	/** The event's own items: rare drops, here for emeralds. */
	private static final List<Ware> RARE_WARES = List.of(
			Ware.of(EventItems.METEORITE_FRAGMENT, 2, 5, 4),
			Ware.of(EventItems.LUCKY_NUGGET, 1, 8, 3),
			Ware.of(EventItems.GOLDEN_SEED, 1, 6, 3),
			Ware.of(EventItems.BOTTLED_LIGHTNING, 1, 12, 2),
			Ware.of(EventItems.BLOODY_TEAR, 1, 20, 1),
			Ware.of(EventItems.MAGMA_HEART, 1, 10, 2));

	/** Hard-to-find vanilla goods. */
	private static final List<Ware> EXOTIC_WARES = List.of(
			Ware.of(Items.NAME_TAG, 1, 5, 3),
			Ware.of(Items.SADDLE, 1, 6, 2),
			Ware.of(Items.EXPERIENCE_BOTTLE, 4, 4, 4),
			Ware.of(Items.GOLDEN_APPLE, 1, 8, 2),
			Ware.of(Items.ENDER_PEARL, 2, 5, 3),
			Ware.of(Items.NAUTILUS_SHELL, 1, 6, 2),
			Ware.of(Items.TOTEM_OF_UNDYING, 1, 40, 1));

	/** The strongest event items, only for event seals: what taking part in events is for. */
	private record Relic(EventItems.EventItem item, int seals) {
	}

	private static final List<Relic> RELICS = List.of(
			new Relic(EventItems.BLOOD_SCYTHE, 8),
			new Relic(EventItems.STARFALL_PICKAXE, 8),
			new Relic(EventItems.WARLORD_AXE, 8),
			new Relic(EventItems.CAPTAINS_CUTLASS, 6),
			new Relic(EventItems.COLOSSUS_HAMMER, 10));

	/** A crop the merchant buys, and how many of it per emerald. */
	private record Purchase(Item crop, int count) {
	}

	// Plain items here: building an ItemCost this early, before the game has loaded its items' data, crashes.
	private static final List<Purchase> HARVEST_PURCHASES = List.of(
			new Purchase(Items.WHEAT, 20),
			new Purchase(Items.CARROT, 22),
			new Purchase(Items.POTATO, 26),
			new Purchase(Items.BEETROOT, 15),
			new Purchase(Items.PUMPKIN, 6),
			new Purchase(Items.MELON, 4));

	private NomadMerchant() {
	}

	/**
	 * Spawns a merchant at {@code pos}, staying at most {@code stayTicks}. Its wares are named in {@code language}
	 * for players without the resource pack, and cost {@code priceMultiplier} times their usual price.
	 */
	public static WanderingTrader spawn(ServerLevel level, BlockPos pos, String language, int stayTicks, double priceMultiplier) {
		WanderingTrader merchant = EntityTypes.WANDERING_TRADER.create(level, EntitySpawnReason.EVENT);
		if (merchant == null) {
			return null;
		}
		merchant.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		merchant.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
		// Without AI it stays put and never drinks the milk that would undo its invisibility.
		merchant.setNoAi(true);
		merchant.setPermanentlyInvulnerable(true);
		// Its llamas leave when this runs out, and it leaves too if the event's own cleanup never runs.
		merchant.setDespawnDelay(stayTicks);
		// Replaces the vanilla trades it rolls on first use (overrideOffers only works on the client).
		MerchantOffers offers = merchant.getOffers();
		offers.clear();
		offers.addAll(offers(level.getRandom(), language, priceMultiplier));
		MobSpawning.name(merchant, NAME_KEY, language, ChatFormatting.YELLOW);
		Creature.mark(merchant, KIND);
		level.addFreshEntity(merchant);
		Creature.dress(merchant, BODY);
		level.sendParticles(ParticleTypes.CLOUD, merchant.getX(), merchant.getY() + 1.0, merchant.getZ(), 20, 0.5, 0.8, 0.5, 0.02);
		return merchant;
	}

	/** Leashes up to {@code count} trader llamas to the merchant, standing around it. */
	public static List<TraderLlama> spawnLlamas(ServerLevel level, WanderingTrader merchant, int count) {
		List<TraderLlama> llamas = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			BlockPos spot = MobSpawning.spotNear(level, EntityTypes.TRADER_LLAMA, merchant.blockPosition(), 2, 4, 2, 8);
			if (spot == null) {
				continue;
			}
			TraderLlama llama = EntityTypes.TRADER_LLAMA.create(level, EntitySpawnReason.EVENT);
			if (llama == null) {
				continue;
			}
			llama.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
			llama.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), EntitySpawnReason.EVENT, null);
			llama.addTag(LLAMA_TAG);
			level.addFreshEntity(llama);
			llama.setLeashedTo(merchant, true);
			llamas.add(llama);
		}
		return llamas;
	}

	public static boolean is(Entity entity) {
		return KIND.is(entity);
	}

	/** The caravan's configured prices, for summoned merchants too. */
	private static double configuredPrices() {
		return ModConfig.get().event(MerchantCaravanEvent.ID).getDouble("priceMultiplier");
	}

	/** Turns towards nearby players and rewards whoever trades with it. */
	private static void act(ServerLevel level, Mob mob, int tick) {
		if (!(mob instanceof WanderingTrader merchant)) {
			return;
		}
		if (tick % 10 == 0 && !merchant.isTrading()) {
			faceNearestPlayer(level, merchant, FACE_PLAYER_RANGE);
		}
		if (merchant.getTradingPlayer() instanceof ServerPlayer trader) {
			LAST_TRADER.put(merchant, trader);
		}
		int trades = tradesMade(merchant);
		// A new merchant's offers start unused; one back from a save has no trader to reward yet.
		Integer before = TRADES.put(merchant, trades);
		ServerPlayer trader = LAST_TRADER.get(merchant);
		if (trades > (before == null ? 0 : before) && trader != null) {
			Advancements.award(trader, "trade_with_caravan");
			EventManager.score(trader, MerchantCaravanEvent.ID, 2);
		}
	}

	public static boolean isCaravanLlama(Entity entity) {
		return entity.entityTags().contains(LLAMA_TAG);
	}

	/** Turns the merchant towards the nearest player within {@code range}, if any. */
	public static void faceNearestPlayer(ServerLevel level, WanderingTrader merchant, double range) {
		ServerPlayer nearest = null;
		double best = range * range;
		for (ServerPlayer player : level.players()) {
			double distance = player.distanceToSqr(merchant);
			if (!player.isSpectator() && distance < best) {
				best = distance;
				nearest = player;
			}
		}
		if (nearest != null) {
			float yaw = (float) (Mth.atan2(nearest.getZ() - merchant.getZ(), nearest.getX() - merchant.getX()) * Mth.RAD_TO_DEG) - 90.0f;
			merchant.setYRot(yaw);
			merchant.setYHeadRot(yaw);
			merchant.setYBodyRot(yaw);
		}
	}

	/** How many trades the merchant has made, counting every offer. */
	public static int tradesMade(WanderingTrader merchant) {
		int uses = 0;
		for (MerchantOffer offer : merchant.getOffers()) {
			uses += offer.getUses();
		}
		return uses;
	}

	/** Ends any trade in progress and removes the merchant with a puff of dust. */
	public static void leave(ServerLevel level, WanderingTrader merchant) {
		if (merchant.getTradingPlayer() instanceof ServerPlayer player) {
			player.closeContainer();
		}
		merchant.setTradingPlayer(null);
		level.sendParticles(ParticleTypes.CLOUD, merchant.getX(), merchant.getY() + 1.0, merchant.getZ(), 20, 0.5, 0.8, 0.5, 0.02);
		Creature.remove(merchant);
	}

	/** A random selection: four of the events' items, three exotic goods, two relics and one crop the merchant buys. */
	private static MerchantOffers offers(RandomSource random, String language, double priceMultiplier) {
		MerchantOffers offers = new MerchantOffers();
		for (Ware ware : pick(RARE_WARES, 4, random)) {
			offers.add(sell(ware, language, priceMultiplier));
		}
		for (Ware ware : pick(EXOTIC_WARES, 3, random)) {
			offers.add(sell(ware, language, priceMultiplier));
		}
		for (Relic relic : pick(RELICS, 2, random)) {
			offers.add(new MerchantOffer(EventItems.sealCost(relic.seals()), relic.item().create(language, 1), 1, 1, 0.0f));
		}
		Purchase purchase = HARVEST_PURCHASES.get(random.nextInt(HARVEST_PURCHASES.size()));
		offers.add(new MerchantOffer(new ItemCost(purchase.crop(), purchase.count()), new ItemStack(Items.EMERALD), 8, 1, PRICE_GROWTH));
		return offers;
	}

	private static MerchantOffer sell(Ware ware, String language, double priceMultiplier) {
		int price = Mth.clamp((int) Math.round(ware.price() * priceMultiplier), 1, 64);
		return new MerchantOffer(new ItemCost(Items.EMERALD, price), ware.result().apply(language), ware.maxUses(), 1, PRICE_GROWTH);
	}

	/** {@code count} different entries of {@code list}, in their original order. */
	private static <T> List<T> pick(List<T> list, int count, RandomSource random) {
		List<T> left = new ArrayList<>(list);
		while (left.size() > count) {
			left.remove(random.nextInt(left.size()));
		}
		return left;
	}
}
