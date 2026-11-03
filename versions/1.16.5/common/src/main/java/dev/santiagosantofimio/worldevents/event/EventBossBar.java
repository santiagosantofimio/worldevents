package dev.santiagosantofimio.worldevents.event;

import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.util.DropMath;
import dev.santiagosantofimio.worldevents.util.LocalizedBossBar;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

/** Boss bar for a running event: its name and time left, for every player in the dimension. */
final class EventBossBar {
	private final WorldEvent event;
	private final LocalizedBossBar bar;

	EventBossBar(WorldEvent event) {
		this.event = event;
		this.bar = new LocalizedBossBar(event.barColor(), BossEvent.BossBarOverlay.PROGRESS, event.darkenSky());
	}

	void update(ServerLevel level, ActiveEvent active) {
		String time = DropMath.formatTicks(active.remainingTicks());
		bar.update(level.players(), language -> {
			String name = Lang.text(language, "event." + event.id() + ".name");
			String detail = event.barDetail(active, language);
			if (detail != null) {
				name = Lang.text(language, "bossbar.title", name, detail);
			}
			return new TextComponent(Lang.text(language, "bossbar.title", name, time)).withStyle(event.textColor());
		}, (float) active.remainingTicks() / active.totalTicks());
	}

	Component titleFor(ServerPlayer player) {
		return bar.titleFor(player);
	}

	void removePlayer(ServerPlayer player) {
		bar.removePlayer(player);
	}

	void clear() {
		bar.clear();
	}
}
