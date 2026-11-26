package dev.santiagosantofimio.worldomens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.santiagosantofimio.worldomens.config.Preset;
import org.junit.jupiter.api.Test;

class PresetTest {
	@Test
	void calmFavoursPeacefulEventsAndEpicFavoursCombat() {
		assertEquals(25, Preset.CALM.scaleWeight(10, true));
		assertEquals(5, Preset.CALM.scaleWeight(10, false));
		assertEquals(10, Preset.NORMAL.scaleWeight(10, true));
		assertEquals(10, Preset.NORMAL.scaleWeight(10, false));
		assertEquals(6, Preset.EPIC.scaleWeight(10, true));
		assertEquals(15, Preset.EPIC.scaleWeight(10, false));
	}

	@Test
	void calmIsSlowerAndEpicIsFaster() {
		assertTrue(Preset.CALM.startChanceFactor() < Preset.NORMAL.startChanceFactor());
		assertTrue(Preset.EPIC.startChanceFactor() > Preset.NORMAL.startChanceFactor());
		assertTrue(Preset.CALM.cooldownFactor() > Preset.NORMAL.cooldownFactor());
		assertTrue(Preset.EPIC.cooldownFactor() < Preset.NORMAL.cooldownFactor());
	}

	@Test
	void namesAreParsedIgnoringCase() {
		assertSame(Preset.EPIC, Preset.parse("EPIC"));
		assertSame(Preset.CALM, Preset.parse("calm"));
		assertNull(Preset.parse("peaceful"));
		assertSame(Preset.NORMAL, Preset.byId("peaceful"));
		assertSame(Preset.NORMAL, Preset.byId("normal"));
	}
}
