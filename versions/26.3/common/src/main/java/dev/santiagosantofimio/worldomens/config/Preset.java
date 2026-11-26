package dev.santiagosantofimio.worldomens.config;

public enum Preset {
	CALM("calm", 0.6, 1.5, 0.5, 250, 50),
	NORMAL("normal", 1.0, 1.0, 1.0, 100, 100),
	EPIC("epic", 1.6, 0.6, 2.0, 60, 150);

	private final String id;
	private final double startChanceFactor;
	private final double cooldownFactor;
	private final double convergenceFactor;
	private final int peacefulWeightPercent;
	private final int combatWeightPercent;

	Preset(String id, double startChanceFactor, double cooldownFactor, double convergenceFactor, int peacefulWeightPercent, int combatWeightPercent) {
		this.id = id;
		this.startChanceFactor = startChanceFactor;
		this.cooldownFactor = cooldownFactor;
		this.convergenceFactor = convergenceFactor;
		this.peacefulWeightPercent = peacefulWeightPercent;
		this.combatWeightPercent = combatWeightPercent;
	}

	public String id() {
		return id;
	}

	public double startChanceFactor() {
		return startChanceFactor;
	}

	public double cooldownFactor() {
		return cooldownFactor;
	}

	public double convergenceFactor() {
		return convergenceFactor;
	}

	public int scaleWeight(int weight, boolean peaceful) {
		return (int) ((long) weight * (peaceful ? peacefulWeightPercent : combatWeightPercent) / 100L);
	}

	public static Preset byId(String id) {
		for (Preset preset : values()) {
			if (preset.id.equalsIgnoreCase(id)) {
				return preset;
			}
		}
		return NORMAL;
	}

	public static Preset parse(String id) {
		for (Preset preset : values()) {
			if (preset.id.equalsIgnoreCase(id)) {
				return preset;
			}
		}
		return null;
	}
}
