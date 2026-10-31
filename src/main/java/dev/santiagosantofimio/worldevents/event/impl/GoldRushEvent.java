package dev.santiagosantofimio.worldevents.event.impl;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldevents.config.EventSettings;
import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.WorldEvent;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.mob.Creature;
import dev.santiagosantofimio.worldevents.mob.MimicSack;
import dev.santiagosantofimio.worldevents.mob.MobSpawning;
import dev.santiagosantofimio.worldevents.util.DeferredDiscard;
import dev.santiagosantofimio.worldevents.util.Drops;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** For a few minutes, ores mined by players drop more. */
public final class GoldRushEvent implements WorldEvent {
	public static final String ID = "gold_rush";
	private static final DustParticleOptions SPARKLE = new DustParticleOptions(0xFFD700, 1.0f);

	@Override
	public String id() {
		return ID;
	}

	private static final class SackState {
		private record Spawned(UUID owner, int spawnedAt) {
		}

		private final Set<UUID> rolled = new HashSet<>();
		private final Map<UUID, Integer> dueAt = new HashMap<>();
		private final Map<Rabbit, Spawned> sacks = new HashMap<>();
	}

	@Override
	public JsonObject defaultSettings() {
		// At most once every 4 in-game days; lasts 5 minutes.
		JsonObject json = EventSettings.defaults(true, 10, 4800, 300);
		json.addProperty("dropMultiplier", 2.0);
		json.addProperty("particles", true);
		json.addProperty("luckyNuggetChance", 0.03);
		json.addProperty("mimicSackChance", 0.5);
		json.addProperty("mimicSackLifetime", 60);
		return json;
	}

	@Override
	public BossEvent.BossBarColor barColor() {
		return BossEvent.BossBarColor.YELLOW;
	}

	@Override
	public ChatFormatting textColor() {
		return ChatFormatting.GOLD;
	}

	@Override
	public Holder<SoundEvent> startSound() {
		return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.PLAYER_LEVELUP);
	}

	@Override
	public void tick(ServerLevel level, ActiveEvent active) {
		SackState state = active.state(SackState::new);
		EventSettings settings = settings();
		RandomSource random = level.getRandom();
		int elapsed = (active.totalTicks() - active.remainingTicks()) / 20;
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) {
				continue;
			}
			UUID id = player.getUUID();
			// Each player gets one roll per gold rush, even if they join late.
			if (state.rolled.add(id) && random.nextDouble() < settings.getDouble("mimicSackChance")) {
				state.dueAt.put(id, elapsed + Mth.nextInt(random, 10, 60));
			}
			Integer due = state.dueAt.get(id);
			if (due != null && elapsed >= due) {
				state.dueAt.remove(id);
				spawnSackNear(level, player, state, elapsed);
			}
		}

		int lifetime = settings.getInt("mimicSackLifetime");
		Iterator<Map.Entry<Rabbit, SackState.Spawned>> sacks = state.sacks.entrySet().iterator();
		while (sacks.hasNext()) {
			Map.Entry<Rabbit, SackState.Spawned> entry = sacks.next();
			Rabbit sack = entry.getKey();
			if (sack.isRemoved()) {
				sacks.remove();
			} else if (elapsed - entry.getValue().spawnedAt() >= lifetime) {
				MimicSack.escape(level, sack);
				sacks.remove();
				ServerPlayer owner = entry.getValue().owner() == null ? null : level.getServer().getPlayerList().getPlayer(entry.getValue().owner());
				if (owner != null) {
					owner.sendSystemMessage(Lang.component(owner, "mob.mimic_sack.escaped").withStyle(ChatFormatting.GRAY));
				}
			}
		}
	}

	private static void spawnSackNear(ServerLevel level, ServerPlayer player, SackState state, int elapsed) {
		BlockPos spot = MobSpawning.spotNear(level, EntityTypes.RABBIT, player.blockPosition(), 5, 12, 3, 10);
		if (spot == null) {
			return;
		}
		Rabbit sack = MimicSack.spawn(level, spot, Lang.language(player));
		if (sack != null) {
			state.sacks.put(sack, new SackState.Spawned(player.getUUID(), elapsed));
			player.sendSystemMessage(Lang.component(player, "mob.mimic_sack.appeared").withStyle(ChatFormatting.GOLD));
		}
	}

	/** Spawns a mimic sack at {@code pos} now, for tests and admins. */
	public Rabbit spawnSack(ServerLevel level, ActiveEvent active, BlockPos pos, String language) {
		Rabbit sack = MimicSack.spawn(level, pos, language);
		if (sack != null) {
			active.state(SackState::new).sacks.put(sack, new SackState.Spawned(null, (active.totalTicks() - active.remainingTicks()) / 20));
		}
		return sack;
	}

	@Override
	public void end(ServerLevel level, ActiveEvent active) {
		SackState state = active.state(SackState::new);
		for (Rabbit sack : state.sacks.keySet()) {
			if (!sack.isRemoved()) {
				MimicSack.escape(level, sack);
			}
		}
		state.sacks.clear();
	}

	@Override
	public void registerHooks() {
		// One hit from a player and the sack bursts into gold instead of taking damage.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof Rabbit sack && MimicSack.is(sack) && source.getEntity() instanceof ServerPlayer player
					&& sack.level() instanceof ServerLevel level) {
				MimicSack.caught(level, sack, player);
				EventManager.score(player, ID, 5);
				return false;
			}
			return true;
		});

		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof Rabbit sack) || !MimicSack.is(sack) || Creature.isSummoned(sack)) {
				return;
			}
			ActiveEvent active = EventManager.active(level);
			if (active != null && active.event() == this) {
				active.state(SackState::new).sacks.putIfAbsent(sack, new SackState.Spawned(null, (active.totalTicks() - active.remainingTicks()) / 20));
			} else {
				DeferredDiscard.discard(sack);
				sack.getPassengers().forEach(DeferredDiscard::discard);
			}
		});

		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
			if (state == null || !state.is(ConventionalBlockTags.ORES)) {
				return;
			}
			if (!(context.getOptional(LootContextParams.THIS_ENTITY) instanceof Player) || !EventManager.isActive(context.getLevel(), this)) {
				return;
			}
			// Silk touch (or ores that always drop themselves, like ancient debris) would just duplicate the block.
			Item blockItem = state.getBlock().asItem();
			for (ItemStack stack : drops) {
				if (stack.is(blockItem)) {
					return;
				}
			}
			Drops.multiply(drops, settings().getDouble("dropMultiplier"), context.getRandom());
			if (context.getRandom().nextDouble() < settings().getDouble("luckyNuggetChance")
					&& context.getOptional(LootContextParams.THIS_ENTITY) instanceof Player miner) {
				drops.add(EventItems.LUCKY_NUGGET.create(Lang.languageOf(miner), 1));
			}
			if (context.getOptional(LootContextParams.THIS_ENTITY) instanceof ServerPlayer miner) {
				EventManager.score(miner, ID, 1);
			}
			Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
			if (origin != null && settings().getBoolean("particles")) {
				// Golden sparkle and a chime so players notice the bonus.
				ServerLevel level = context.getLevel();
				level.sendParticles(SPARKLE, origin.x, origin.y, origin.z, 12, 0.35, 0.35, 0.35, 0.0);
				level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 1.4f);
			}
		});
	}
}
