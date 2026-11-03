package dev.santiagosantofimio.worldevents.event.impl;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldevents.config.EventSettings;
import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.WorldEvent;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.mob.Creature;
import dev.santiagosantofimio.worldevents.mob.MobSpawning;
import dev.santiagosantofimio.worldevents.mob.NomadMerchant;
import dev.santiagosantofimio.worldevents.platform.Hooks;
import dev.santiagosantofimio.worldevents.util.DayCycle;
import dev.santiagosantofimio.worldevents.util.DeferredDiscard;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.npc.WanderingTrader;

/**
 * A merchant caravan: a nomad merchant and their llamas set up camp near players for ten minutes, selling the
 * events' rare items and other hard-to-find goods for emeralds. With players spread out, a second caravan can
 * come to the other group.
 */
public final class MerchantCaravanEvent implements WorldEvent {
	public static final String ID = "merchant_caravan";
	private static final long EARLIEST_START = 1000;
	private static final long LATEST_START = 8000;
	/** Caravans arrive in the first minute; after that, no new ones. */
	private static final int ARRIVAL_SECONDS = 60;
	/** A player this close to a caravan does not get another one. */
	private static final double CARAVAN_REACH = 64.0;
	/** The merchant leaves on its own this long after the event, in case the event's cleanup never runs. */
	private static final int STAY_MARGIN_TICKS = 2400;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public JsonObject defaultSettings() {
		// At most once every 5 in-game days; lasts 10 minutes.
		JsonObject json = EventSettings.defaults(true, 8, 6000, 600);
		json.addProperty("maxCaravans", 2);
		json.addProperty("llamas", 2);
		json.addProperty("priceMultiplier", 1.0);
		return json;
	}

	@Override
	public BossEvent.BossBarColor barColor() {
		return BossEvent.BossBarColor.WHITE;
	}

	@Override
	public ChatFormatting textColor() {
		return ChatFormatting.YELLOW;
	}

	@Override
	public SoundEvent startSound() {
		return SoundEvents.WANDERING_TRADER_YES;
	}

	/** Caravans travel the overworld. */
	@Override
	public boolean isAvailable(ServerLevel level) {
		return DayCycle.hasDayCycle(level);
	}

	/** In the morning, so the caravan leaves before nightfall. */
	@Override
	public boolean canStart(ServerLevel level) {
		return !level.isNight() && DayCycle.isBetween(level, EARLIEST_START, LATEST_START);
	}

	@Override
	public void tick(ServerLevel level, ActiveEvent active) {
		CaravanState state = active.state(CaravanState::new);
		state.merchants.removeIf(merchant -> merchant.removed || !merchant.isAlive());
		state.llamas.removeIf(llama -> llama.removed || !llama.isAlive());
		int elapsed = (active.totalTicks() - active.remainingTicks()) / 20;
		if (elapsed > ARRIVAL_SECONDS) {
			return;
		}
		EventSettings settings = settings();
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || state.merchants.size() >= settings.getInt("maxCaravans") || hasCaravanNear(state, player)) {
				continue;
			}
			BlockPos spot = MobSpawning.spotNear(level, EntityType.WANDERING_TRADER, player.blockPosition(), 6, 12, 4, 10);
			if (spot != null && spawnCaravan(level, active, spot, Lang.language(player)) != null) {
				for (ServerPlayer other : level.players()) {
					other.sendMessage(Lang.component(other, "mob.nomad_merchant.arrived", player.getName().getString()).withStyle(textColor()), net.minecraft.Util.NIL_UUID);
				}
			}
		}
	}

	@Override
	public void end(ServerLevel level, ActiveEvent active) {
		CaravanState state = active.state(CaravanState::new);
		// Llamas first: once the merchant is gone, their leads would drop as items.
		for (TraderLlama llama : state.llamas) {
			if (!llama.removed) {
				leaveOrStay(level, llama);
			}
		}
		for (WanderingTrader merchant : state.merchants) {
			if (!merchant.removed) {
				NomadMerchant.leave(level, merchant);
			}
		}
		state.llamas.clear();
		state.merchants.clear();
	}

	/** A llama still with the caravan leaves with it; one a player has taken stays as an ordinary trader llama. */
	private static void leaveOrStay(ServerLevel level, TraderLlama llama) {
		boolean taken = llama.isTamed() || llama.isLeashed() && !(llama.getLeashHolder() instanceof WanderingTrader holder && NomadMerchant.is(holder));
		llama.removeTag(NomadMerchant.LLAMA_TAG);
		if (!taken) {
			llama.dropLeash(true, false);
			level.sendParticles(ParticleTypes.CLOUD, llama.getX(), llama.getY() + 1.0, llama.getZ(), 10, 0.4, 0.6, 0.4, 0.02);
			llama.remove();
		}
	}

	private static boolean hasCaravanNear(CaravanState state, ServerPlayer player) {
		for (WanderingTrader merchant : state.merchants) {
			if (merchant.distanceToSqr(player) < CARAVAN_REACH * CARAVAN_REACH) {
				return true;
			}
		}
		return false;
	}

	/** Sets up a caravan at {@code pos} now: the merchant and their llamas. Also for tests and admins. */
	public WanderingTrader spawnCaravan(ServerLevel level, ActiveEvent active, BlockPos pos, String language) {
		EventSettings settings = settings();
		WanderingTrader merchant = NomadMerchant.spawn(level, pos, language, active.remainingTicks() + STAY_MARGIN_TICKS,
				settings.getDouble("priceMultiplier"));
		if (merchant == null) {
			return null;
		}
		CaravanState state = active.state(CaravanState::new);
		// Usually already tracked: the load callback also sees new entities.
		if (!state.merchants.contains(merchant)) {
			state.merchants.add(merchant);
		}
		for (TraderLlama llama : NomadMerchant.spawnLlamas(level, merchant, settings.getInt("llamas"))) {
			if (!state.llamas.contains(llama)) {
				state.llamas.add(llama);
			}
		}
		return merchant;
	}

	/** The caravan llamas of the current run that are still around. */
	public static List<TraderLlama> llamas(ActiveEvent active) {
		CaravanState state = active.state(CaravanState::new);
		state.llamas.removeIf(llama -> llama.removed || !llama.isAlive());
		return List.copyOf(state.llamas);
	}

	@Override
	public void registerHooks() {
		Hooks.ENTITY_LOAD.register((entity, level) -> {
			boolean merchant = entity instanceof WanderingTrader && NomadMerchant.is(entity) && !Creature.isSummoned(entity);
			boolean llama = entity instanceof TraderLlama && NomadMerchant.isCaravanLlama(entity);
			if (!merchant && !llama) {
				return;
			}
			ActiveEvent active = EventManager.active(level);
			if (active != null && active.event() == this) {
				CaravanState state = active.state(CaravanState::new);
				if (entity instanceof WanderingTrader trader && merchant && !state.merchants.contains(trader)) {
					state.merchants.add(trader);
				} else if (entity instanceof TraderLlama traderLlama && llama && !state.llamas.contains(traderLlama)) {
					state.llamas.add(traderLlama);
				}
			} else if (merchant || !((TraderLlama) entity).isTamed()) {
				// Back after the caravan left: it goes too.
				DeferredDiscard.discard(entity);
				entity.getPassengers().forEach(DeferredDiscard::discard);
			}
		});

		// Nothing hurts the merchant but /kill, the void and players in creative (as with vanilla invulnerable mobs).
		Hooks.ALLOW_DAMAGE.register((entity, source, amount) ->
				!NomadMerchant.is(entity) || source.isBypassInvul() || source.isCreativePlayer());
	}

	private static final class CaravanState {
		private final List<WanderingTrader> merchants = new ArrayList<>();
		private final List<TraderLlama> llamas = new ArrayList<>();
	}
}
