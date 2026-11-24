package dev.santiagosantofimio.worldevents.i18n;

import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.Map;

/** Language tables and lookup rules, with no dependency on Minecraft so it can be unit tested. */
public final class Translations {
	public static final String DEFAULT_LANGUAGE = "en_us";

	private final Map<String, Map<String, String>> tables;

	public Translations(Map<String, Map<String, String>> tables) {
		this.tables = Map.copyOf(tables);
	}

	/**
	 * Maps a client language code to one we have: exact match, then any table with the same language
	 * prefix (es_mx uses es_es), then {@link #DEFAULT_LANGUAGE}.
	 */
	public String resolveLanguage(String requested) {
		if (requested == null || requested.isEmpty()) {
			return DEFAULT_LANGUAGE;
		}
		String code = requested.toLowerCase(Locale.ROOT);
		if (tables.containsKey(code)) {
			return code;
		}
		int underscore = code.indexOf('_');
		String prefix = (underscore > 0 ? code.substring(0, underscore) : code) + "_";
		String best = null;
		for (String candidate : tables.keySet()) {
			if (candidate.startsWith(prefix) && (best == null || candidate.compareTo(best) < 0)) {
				best = candidate;
			}
		}
		return best != null ? best : DEFAULT_LANGUAGE;
	}

	/** Returns the text for {@code key}, falling back to English and then to the key itself. */
	public String get(String language, String key) {
		Map<String, String> table = tables.get(resolveLanguage(language));
		String value = table != null ? table.get(key) : null;
		if (value == null) {
			Map<String, String> fallback = tables.get(DEFAULT_LANGUAGE);
			value = fallback != null ? fallback.get(key) : null;
		}
		return value != null ? value : key;
	}

	/** Like {@link #get} but fills {@code %s} / {@code %1$s} placeholders. */
	public String format(String language, String key, Object... args) {
		String pattern = get(language, key);
		if (args.length == 0) {
			return pattern;
		}
		try {
			return String.format(Locale.ROOT, pattern, args);
		} catch (IllegalFormatException e) {
			return pattern;
		}
	}
}
