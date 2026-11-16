package dev.santiagosantofimio.worldevents;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.santiagosantofimio.worldevents.i18n.Translations;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TranslationsTest {
	private final Translations translations = new Translations(Map.of(
			"en_us", Map.of("hello", "Hello %s", "only_en", "English"),
			"es_es", Map.of("hello", "Hola %s")));

	@Test
	void resolvesExactPrefixAndDefault() {
		assertEquals("es_es", translations.resolveLanguage("es_es"));
		assertEquals("es_es", translations.resolveLanguage("ES_MX"));
		assertEquals("en_us", translations.resolveLanguage("en_gb"));
		assertEquals("en_us", translations.resolveLanguage("fr_fr"));
		assertEquals("en_us", translations.resolveLanguage(null));
	}

	@Test
	void fallsBackToEnglishThenKey() {
		assertEquals("Hola Steve", translations.format("es_ar", "hello", "Steve"));
		assertEquals("English", translations.get("es_es", "only_en"));
		assertEquals("missing.key", translations.get("es_es", "missing.key"));
	}

	@Test
	void badPatternReturnsRawText() {
		Translations broken = new Translations(Map.of("en_us", Map.of("k", "100%")));
		assertEquals("100%", broken.format("en_us", "k", 1));
	}
}
