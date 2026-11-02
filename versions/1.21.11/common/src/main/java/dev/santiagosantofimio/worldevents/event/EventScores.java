package dev.santiagosantofimio.worldevents.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** What each player did during a running event. Saved with the event, so a restart does not lose it. */
public final class EventScores {
	private static final String KEY = "scores";
	private static final String NAME = "name";
	private static final String POINTS = "points";

	/** A player's standing; the name is kept so the ranking still shows players who have left. */
	public record Entry(UUID player, String name, int points) {
	}

	private EventScores() {
	}

	static void add(ActiveEvent active, ServerPlayer player, int points) {
		CompoundTag scores = active.data().getCompoundOrEmpty(KEY);
		String id = player.getUUID().toString();
		CompoundTag entry = scores.getCompoundOrEmpty(id);
		entry.putString(NAME, player.getName().getString());
		entry.putInt(POINTS, entry.getIntOr(POINTS, 0) + points);
		scores.put(id, entry);
		active.data().put(KEY, scores);
	}

	/** Every player who scored, best first. */
	public static List<Entry> ranking(ActiveEvent active) {
		CompoundTag scores = active.data().getCompoundOrEmpty(KEY);
		List<Entry> entries = new ArrayList<>();
		for (String id : scores.keySet()) {
			CompoundTag entry = scores.getCompoundOrEmpty(id);
			entries.add(new Entry(UUID.fromString(id), entry.getStringOr(NAME, "?"), entry.getIntOr(POINTS, 0)));
		}
		entries.sort(Comparator.comparingInt(Entry::points).reversed());
		return entries;
	}
}
