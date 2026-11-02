package dev.santiagosantofimio.worldevents.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

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
			// Lenient so a save with an event id that no longer exists loads as "no event" instead of failing.
			ActiveEvent.CODEC.lenientOptionalFieldOf("active").forGetter(data -> Optional.ofNullable(data.active)),
			Plan.CODEC.lenientOptionalFieldOf("planned").forGetter(data -> Optional.ofNullable(data.plan)),
			Codec.LONG.optionalFieldOf("lastEventEnd").forGetter(data -> Optional.ofNullable(data.lastEventEnd)),
			Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("lastEnds", Map.of()).forGetter(data -> data.lastEnds)
	).apply(instance, WorldEventsData::new));

	// No DataFixTypes: Fabric API allows null and skips data fixing for mod data.
	private static final SavedDataType<WorldEventsData> TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath(WorldEventsMod.MOD_ID, "state"), WorldEventsData::new, CODEC, null);

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
		return level.getDataStorage().computeIfAbsent(TYPE);
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
