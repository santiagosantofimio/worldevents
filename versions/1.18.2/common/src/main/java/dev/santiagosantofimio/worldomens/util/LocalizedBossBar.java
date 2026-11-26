package dev.santiagosantofimio.worldomens.util;

import dev.santiagosantofimio.worldomens.i18n.Lang;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;

public final class LocalizedBossBar {
	private final BossEvent.BossBarColor color;
	private final BossEvent.BossBarOverlay overlay;
	private final boolean darkenSky;
	private final Map<String, ServerBossEvent> bars = new HashMap<>();

	public LocalizedBossBar(BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay, boolean darkenSky) {
		this.color = color;
		this.overlay = overlay;
		this.darkenSky = darkenSky;
	}

	public void update(Collection<ServerPlayer> viewers, Function<String, Component> title, float progress) {
		for (Map.Entry<String, ServerBossEvent> entry : bars.entrySet()) {
			for (ServerPlayer player : new ArrayList<>(entry.getValue().getPlayers())) {
				if (!viewers.contains(player) || !Lang.language(player).equals(entry.getKey())) {
					entry.getValue().removePlayer(player);
				}
			}
		}
		for (ServerPlayer player : viewers) {
			ServerBossEvent bar = bars.computeIfAbsent(Lang.language(player), language -> create());
			if (!bar.getPlayers().contains(player)) {
				bar.addPlayer(player);
			}
		}
		float clamped = Mth.clamp(progress, 0.0f, 1.0f);
		for (Map.Entry<String, ServerBossEvent> entry : bars.entrySet()) {
			entry.getValue().setName(title.apply(entry.getKey()));
			entry.getValue().setProgress(clamped);
		}
	}

	public Component titleFor(ServerPlayer player) {
		for (ServerBossEvent bar : bars.values()) {
			if (bar.getPlayers().contains(player)) {
				return bar.getName();
			}
		}
		return null;
	}

	public void removePlayer(ServerPlayer player) {
		for (ServerBossEvent bar : bars.values()) {
			bar.removePlayer(player);
		}
	}

	public void clear() {
		for (ServerBossEvent bar : bars.values()) {
			bar.removeAllPlayers();
		}
		bars.clear();
	}

	private ServerBossEvent create() {
		ServerBossEvent bar = new ServerBossEvent(TextComponent.EMPTY.copy(), color, overlay);
		bar.setDarkenScreen(darkenSky);
		return bar;
	}
}
