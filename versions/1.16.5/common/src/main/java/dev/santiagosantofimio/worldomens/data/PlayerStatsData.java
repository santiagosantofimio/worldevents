package dev.santiagosantofimio.worldomens.data;

import dev.santiagosantofimio.worldomens.WorldOmensMod;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class PlayerStatsData extends SavedData {
	public record Stats(int events, int points, int seals, int bosses) {
		public static final Stats NONE = new Stats(0, 0, 0, 0);

		public Stats plus(int events, int points, int seals, int bosses) {
			return new Stats(this.events + events, this.points + points, this.seals + seals, this.bosses + bosses);
		}
	}

	private static final String FILE = WorldOmensMod.MOD_ID + "_stats";

	private final Map<String, Stats> stats = new HashMap<>();

	private PlayerStatsData() {
		super(FILE);
	}

	public static PlayerStatsData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(PlayerStatsData::new, FILE);
	}

	@Override
	public void load(CompoundTag tag) {
		PlayerStatsData data = this;
		CompoundTag all = tag.getCompound("stats");
		for (String key : all.getAllKeys()) {
			CompoundTag player = all.getCompound(key);
			data.stats.put(key, new Stats(player.getInt("events"), player.getInt("points"), player.getInt("seals"), player.getInt("bosses")));
		}
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		CompoundTag all = new CompoundTag();
		for (Map.Entry<String, Stats> entry : stats.entrySet()) {
			CompoundTag player = new CompoundTag();
			player.putInt("events", entry.getValue().events());
			player.putInt("points", entry.getValue().points());
			player.putInt("seals", entry.getValue().seals());
			player.putInt("bosses", entry.getValue().bosses());
			all.put(entry.getKey(), player);
		}
		tag.put("stats", all);
		return tag;
	}

	public Stats stats(UUID player) {
		return stats.getOrDefault(player.toString(), Stats.NONE);
	}

	public void add(UUID player, int events, int points, int seals, int bosses) {
		stats.merge(player.toString(), Stats.NONE.plus(events, points, seals, bosses), (old, added) -> old.plus(added.events(), added.points(), added.seals(), added.bosses()));
		setDirty();
	}
}
