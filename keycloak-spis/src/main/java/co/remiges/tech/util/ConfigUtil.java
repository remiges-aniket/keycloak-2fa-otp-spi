package co.remiges.tech.util;

import java.util.Map;

/**
 * Reads values from an authenticator config map (AuthenticatorConfigModel.getConfig()).
 * A key that is missing or only whitespace counts as not set, because the admin
 * console saves a cleared field as an empty string.
 */
public final class ConfigUtil {

	private ConfigUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	/**
	 * @return the configured value, or defaultValue when the key is missing or blank
	 */
	public static String get(Map<String, String> config, String key, String defaultValue) {
		String value = config == null ? null : config.get(key);
		return isBlank(value) ? defaultValue : value;
	}

	/**
	 * @return the first configured value among the keys, or defaultValue when none is set.
	 *         Used for fallbacks such as otpEmailFromName, then brandingName.
	 */
	public static String getFirst(Map<String, String> config, String defaultValue, String... keys) {
		for (String key : keys) {
			String value = config == null ? null : config.get(key);
			if (!isBlank(value)) {
				return value;
			}
		}
		return defaultValue;
	}

	public static boolean getBoolean(Map<String, String> config, String key, boolean defaultValue) {
		String value = config == null ? null : config.get(key);
		return isBlank(value) ? defaultValue : Boolean.parseBoolean(value.trim());
	}

	/**
	 * @return the configured integer, or defaultValue when the key is missing, blank or not a number
	 */
	public static int getInt(Map<String, String> config, String key, int defaultValue) {
		String value = config == null ? null : config.get(key);
		if (isBlank(value)) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
