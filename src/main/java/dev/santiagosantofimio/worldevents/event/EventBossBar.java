package dev.santiagosantofimio.worldevents.event;

import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.util.DropMath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

/**
 * Boss bar for a running event. A boss bar has a single title for everyone, so there is one bar per client
 * language and each player sees the one in their language.
 */
final class EventBossBar {
	private final WorldEvent event;
	private final Map<String, ServerBossEvent> bars = new HashMap<>();

	EventBossBar(WorldEvent event) {
		this.event = event;
	}

	/** Refreshes title and progress, and makes sure exactly the players in {@code level} see the bar. */
	void update(ServerLevel level, ActiveEvent active) {
		List<ServerPlayer> players = level.players();
		for (Map.Entry<String, ServerBossEvent> entry : bars.entrySet()) {
			for (ServerPlayer player : new ArrayList<>(entry.getValue().getPlayers())) {
				if (!players.contains(player) || !Lang.language(player).equals(entry.getKey())) {
					entry.getValue().removePlayer(player);
				}
			}
		}
		for (ServerPlayer player : players) {
			ServerBossEvent bar = bars.computeIfAbsent(Lang.language(player), language -> create());
			if (!bar.getPlayers().contains(player)) {
				bar.addPlayer(player);
			}
		}

		float progress = Math.clamp((float) active.remainingTicks() / active.totalTicks(), 0.0f, 1.0f);
		String time = DropMath.formatTicks(active.remainingTicks());
		for (Map.Entry<String, ServerBossEvent> entry : bars.entrySet()) {
			String language = entry.getKey();
			String name = Lang.text(language, "event." + event.id() + ".name");
			entry.getValue().setName(Component.literal(Lang.text(language, "bossbar.title", name, time)).withStyle(event.textColor()));
			entry.getValue().setProgress(progress);
		}
	}

	void removePlayer(ServerPlayer player) {
		for (ServerBossEvent bar : bars.values()) {
			bar.removePlayer(player);
		}
	}

	void clear() {
		for (ServerBossEvent bar : bars.values()) {
			bar.removeAllPlayers();
		}
		bars.clear();
	}

	private ServerBossEvent create() {
		ServerBossEvent bar = new ServerBossEvent(UUID.randomUUID(), Component.empty(), event.barColor(), BossEvent.BossBarOverlay.PROGRESS);
		bar.setDarkenScreen(event.darkenSky());
		return bar;
	}
}
