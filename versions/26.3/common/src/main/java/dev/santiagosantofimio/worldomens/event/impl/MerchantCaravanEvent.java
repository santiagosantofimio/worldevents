package dev.santiagosantofimio.worldomens.event.impl;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldomens.config.EventSettings;
import dev.santiagosantofimio.worldomens.event.ActiveEvent;
import dev.santiagosantofimio.worldomens.event.EventManager;
import dev.santiagosantofimio.worldomens.event.WorldEvent;
import dev.santiagosantofimio.worldomens.i18n.Lang;
import dev.santiagosantofimio.worldomens.mob.Creature;
import dev.santiagosantofimio.worldomens.mob.MobSpawning;
import dev.santiagosantofimio.worldomens.mob.NomadMerchant;
import dev.santiagosantofimio.worldomens.platform.Hooks;
import dev.santiagosantofimio.worldomens.util.DayCycle;
import dev.santiagosantofimio.worldomens.util.DeferredDiscard;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;

public final class MerchantCaravanEvent implements WorldEvent {
	public static final String ID = "merchant_caravan";
	private static final long EARLIEST_START = 1000;
	private static final long LATEST_START = 8000;
	private static final int ARRIVAL_SECONDS = 60;
	private static final double CARAVAN_REACH = 64.0;
	private static final int STAY_MARGIN_TICKS = 2400;

	@Override
	public boolean peaceful() {
		return true;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public JsonObject defaultSettings() {
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
	public Holder<SoundEvent> startSound() {
		return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.WANDERING_TRADER_YES);
	}

	@Override
	public boolean isAvailable(ServerLevel level) {
		return DayCycle.hasDayCycle(level);
	}

	@Override
	public boolean canStart(ServerLevel level) {
		return !level.isDarkOutside() && DayCycle.isBetween(level, EARLIEST_START, LATEST_START);
	}

	@Override
	public void tick(ServerLevel level, ActiveEvent active) {
		CaravanState state = active.state(CaravanState::new);
		state.merchants.removeIf(merchant -> merchant.isRemoved() || !merchant.isAlive());
		state.llamas.removeIf(llama -> llama.isRemoved() || !llama.isAlive());
		int elapsed = (active.totalTicks() - active.remainingTicks()) / 20;
		if (elapsed > ARRIVAL_SECONDS) {
			return;
		}
		EventSettings settings = settings();
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || state.merchants.size() >= settings.getInt("maxCaravans") || hasCaravanNear(state, player)) {
				continue;
			}
			BlockPos spot = MobSpawning.spotNear(level, EntityTypes.WANDERING_TRADER, player.blockPosition(), 6, 12, 4, 10);
			if (spot != null && spawnCaravan(level, active, spot, Lang.language(player)) != null) {
				for (ServerPlayer other : level.players()) {
					other.sendSystemMessage(Lang.component(other, "mob.nomad_merchant.arrived", player.getName().getString()).withStyle(textColor()));
				}
			}
		}
	}

	@Override
	public void end(ServerLevel level, ActiveEvent active) {
		CaravanState state = active.state(CaravanState::new);
		for (TraderLlama llama : state.llamas) {
			if (!llama.isRemoved()) {
				leaveOrStay(level, llama);
			}
		}
		for (WanderingTrader merchant : state.merchants) {
			if (!merchant.isRemoved()) {
				NomadMerchant.leave(level, merchant);
			}
		}
		state.llamas.clear();
		state.merchants.clear();
	}

	private static void leaveOrStay(ServerLevel level, TraderLlama llama) {
		boolean taken = llama.isTamed() || llama.isLeashed() && !(llama.getLeashHolder() instanceof WanderingTrader holder && NomadMerchant.is(holder));
		llama.removeTag(NomadMerchant.LLAMA_TAG);
		if (!taken) {
			llama.removeLeash();
			level.sendParticles(ParticleTypes.CLOUD, llama.getX(), llama.getY() + 1.0, llama.getZ(), 10, 0.4, 0.6, 0.4, 0.02);
			llama.discard();
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

	public WanderingTrader spawnCaravan(ServerLevel level, ActiveEvent active, BlockPos pos, String language) {
		EventSettings settings = settings();
		WanderingTrader merchant = NomadMerchant.spawn(level, pos, language, active.remainingTicks() + STAY_MARGIN_TICKS,
				settings.getDouble("priceMultiplier"));
		if (merchant == null) {
			return null;
		}
		CaravanState state = active.state(CaravanState::new);
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

	public static List<TraderLlama> llamas(ActiveEvent active) {
		CaravanState state = active.state(CaravanState::new);
		state.llamas.removeIf(llama -> llama.isRemoved() || !llama.isAlive());
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
				DeferredDiscard.discard(entity);
				entity.getPassengers().forEach(DeferredDiscard::discard);
			}
		});

		Hooks.ALLOW_DAMAGE.register((entity, source, amount) ->
				!NomadMerchant.is(entity) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.isCreativePlayer());
	}

	private static final class CaravanState {
		private final List<WanderingTrader> merchants = new ArrayList<>();
		private final List<TraderLlama> llamas = new ArrayList<>();
	}
}
