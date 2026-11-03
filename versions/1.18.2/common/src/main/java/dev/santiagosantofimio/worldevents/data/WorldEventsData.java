package dev.santiagosantofimio.worldevents.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-dimension event state: the running event, the next planned one, and when each event last ended
 * (in game time).
 */
public final class WorldEventsData extends SavedData {
	/** The event drawn to run next, waiting for the right moment to start. */
	public record Plan(String eventId, long plannedAt, long warnedAt) {
		private static final long NOT_WARNED = -1;
		private static final Codec<Plan> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("id").forGetter(Plan::eventId),
				Codec.LONG.fieldOf("at").forGetter(Plan::plannedAt),
				Codec.LONG.optionalFieldOf("warned", NOT_WARNED).forGetter(Plan::warnedAt)
		).apply(instance, Plan::new));

		public Plan(String eventId, long plannedAt) {
			this(eventId, plannedAt, NOT_WARNED);
		}

		public boolean warned() {
			return warnedAt != NOT_WARNED;
		}

		public Plan warnedAt(long gameTime) {
			return new Plan(eventId, plannedAt, gameTime);
		}
	}

	private static final Codec<WorldEventsData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ActiveEvent.CODEC.optionalFieldOf("active").forGetter(data -> Optional.ofNullable(data.active)),
			Plan.CODEC.optionalFieldOf("planned").forGetter(data -> Optional.ofNullable(data.plan)),
			Codec.LONG.optionalFieldOf("lastEventEnd").forGetter(data -> Optional.ofNullable(data.lastEventEnd)),
			Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("lastEnds", Map.of()).forGetter(data -> data.lastEnds)
	).apply(instance, WorldEventsData::new));

	/** File name in the dimension's data folder. */
	private static final String FILE = WorldEventsMod.MOD_ID + "_state";

	private ActiveEvent active;
	private Plan plan;
	private Long lastEventEnd;
	private final Map<String, Long> lastEnds;

	private WorldEventsData() {
		this(Optional.empty(), Optional.empty(), Optional.empty(), Map.of());
	}

	private WorldEventsData(Optional<ActiveEvent> active, Optional<Plan> plan, Optional<Long> lastEventEnd, Map<String, Long> lastEnds) {
		this.active = active.orElse(null);
		this.plan = plan.orElse(null);
		this.lastEventEnd = lastEventEnd.orElse(null);
		this.lastEnds = new HashMap<>(lastEnds);
	}

	public static WorldEventsData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(WorldEventsData::load, WorldEventsData::new, FILE);
	}

	/** Reads the saved state; a save that cannot be read (an event id that no longer exists…) starts over empty. */
	private static WorldEventsData load(CompoundTag tag) {
		return CODEC.parse(NbtOps.INSTANCE, tag.getCompound("state"))
				.resultOrPartial(error -> WorldEventsMod.LOGGER.warn("Could not read the world events state: {}", error))
				.orElseGet(WorldEventsData::new);
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		CODEC.encodeStart(NbtOps.INSTANCE, this)
				.resultOrPartial(error -> WorldEventsMod.LOGGER.error("Could not save the world events state: {}", error))
				.ifPresent(state -> tag.put("state", state));
		return tag;
	}

	public ActiveEvent active() {
		return active;
	}

	public void setActive(ActiveEvent active) {
		this.active = active;
		setDirty();
	}

	public Plan plan() {
		return plan;
	}

	public void setPlan(Plan plan) {
		this.plan = plan;
		setDirty();
	}

	/** Game time when the last event in this dimension ended, or {@code null} if none has run yet. */
	public Long lastEventEnd() {
		return lastEventEnd;
	}

	public Long lastEnd(String eventId) {
		return lastEnds.get(eventId);
	}

	public void recordEnd(String eventId, long gameTime) {
		lastEventEnd = gameTime;
		lastEnds.put(eventId, gameTime);
		setDirty();
	}

	/** Starts the global cooldown from now, so a freshly installed mod does not fire an event right away. */
	public void initCooldownIfNew(long gameTime) {
		if (lastEventEnd == null) {
			lastEventEnd = gameTime;
			setDirty();
		}
	}
}
