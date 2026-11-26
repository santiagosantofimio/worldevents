package dev.santiagosantofimio.worldomens.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class EventScores {
	private static final String KEY = "scores";
	private static final String NAME = "name";
	private static final String POINTS = "points";

	public record Entry(UUID player, String name, int points) {
	}

	private EventScores() {
	}

	static void add(ActiveEvent active, ServerPlayer player, int points) {
		CompoundTag scores = active.data().getCompound(KEY);
		String id = player.getUUID().toString();
		CompoundTag entry = scores.getCompound(id);
		entry.putString(NAME, player.getName().getString());
		entry.putInt(POINTS, entry.getInt(POINTS) + points);
		scores.put(id, entry);
		active.data().put(KEY, scores);
	}

	public static List<Entry> ranking(ActiveEvent active) {
		CompoundTag scores = active.data().getCompound(KEY);
		List<Entry> entries = new ArrayList<>();
		for (String id : scores.getAllKeys()) {
			CompoundTag entry = scores.getCompound(id);
			entries.add(new Entry(UUID.fromString(id), (entry.contains(NAME) ? entry.getString(NAME) : "?"), entry.getInt(POINTS)));
		}
		entries.sort(Comparator.comparingInt(Entry::points).reversed());
		return entries;
	}
}
