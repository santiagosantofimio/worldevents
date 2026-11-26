package dev.santiagosantofimio.worldomens.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.santiagosantofimio.worldomens.WorldOmensMod;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class PlayerStatsData extends SavedData {
	public record Stats(int events, int points, int seals, int bosses) {
		public static final Stats NONE = new Stats(0, 0, 0, 0);
		private static final Codec<Stats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.optionalFieldOf("events", 0).forGetter(Stats::events),
				Codec.INT.optionalFieldOf("points", 0).forGetter(Stats::points),
				Codec.INT.optionalFieldOf("seals", 0).forGetter(Stats::seals),
				Codec.INT.optionalFieldOf("bosses", 0).forGetter(Stats::bosses)
		).apply(instance, Stats::new));

		public Stats plus(int events, int points, int seals, int bosses) {
			return new Stats(this.events + events, this.points + points, this.seals + seals, this.bosses + bosses);
		}
	}

	private static final Codec<PlayerStatsData> CODEC = Codec.unboundedMap(Codec.STRING, Stats.CODEC)
			.xmap(PlayerStatsData::new, data -> data.stats);

	private static final SavedDataType<PlayerStatsData> TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath(WorldOmensMod.MOD_ID, "stats"), PlayerStatsData::new, CODEC, null);

	private final Map<String, Stats> stats;

	private PlayerStatsData() {
		this(Map.of());
	}

	private PlayerStatsData(Map<String, Stats> stats) {
		this.stats = new HashMap<>(stats);
	}

	public static PlayerStatsData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public Stats stats(UUID player) {
		return stats.getOrDefault(player.toString(), Stats.NONE);
	}

	public void add(UUID player, int events, int points, int seals, int bosses) {
		stats.merge(player.toString(), Stats.NONE.plus(events, points, seals, bosses), (old, added) -> old.plus(added.events(), added.points(), added.seals(), added.bosses()));
		setDirty();
	}
}
