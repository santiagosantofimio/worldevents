package dev.santiagosantofimio.worldevents.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;

/** A running event in one dimension. Time and {@link #data()} are saved; {@link #state} is rebuilt after restarts. */
public final class ActiveEvent {
	private static final Codec<WorldEvent> EVENT_CODEC = Codec.STRING.comapFlatMap(
			id -> {
				WorldEvent event = EventRegistry.get(id);
				return event != null ? DataResult.success(event) : DataResult.error("Unknown world event: " + id);
			},
			WorldEvent::id);

	public static final Codec<ActiveEvent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			EVENT_CODEC.fieldOf("id").forGetter(ActiveEvent::event),
			Codec.INT.fieldOf("total").forGetter(ActiveEvent::totalTicks),
			Codec.INT.fieldOf("remaining").forGetter(ActiveEvent::remainingTicks),
			CompoundTag.CODEC.optionalFieldOf("data", new CompoundTag()).forGetter(ActiveEvent::data)
	).apply(instance, ActiveEvent::new));

	private final WorldEvent event;
	private final int totalTicks;
	private int remainingTicks;
	private final CompoundTag data;

	private EventBossBar bossBar;
	private Object state;

	public ActiveEvent(WorldEvent event, int totalTicks, int remainingTicks, CompoundTag data) {
		this.event = event;
		this.totalTicks = Math.max(1, totalTicks);
		this.remainingTicks = remainingTicks;
		this.data = data.copy();
	}

	public WorldEvent event() {
		return event;
	}

	public int totalTicks() {
		return totalTicks;
	}

	public int remainingTicks() {
		return remainingTicks;
	}

	void countDown(int ticks) {
		remainingTicks = Math.max(0, remainingTicks - ticks);
	}

	/** Event-specific values that survive restarts. Mark the saved data dirty after changing it. */
	public CompoundTag data() {
		return data;
	}

	/** Event-specific runtime state (tracked mobs, timers…). Lost on restart, so {@code factory} recreates it. */
	@SuppressWarnings("unchecked")
	public <T> T state(Supplier<T> factory) {
		if (state == null) {
			state = factory.get();
		}
		return (T) state;
	}

	EventBossBar bossBar() {
		return bossBar;
	}

	void setBossBar(EventBossBar bossBar) {
		this.bossBar = bossBar;
	}
}
