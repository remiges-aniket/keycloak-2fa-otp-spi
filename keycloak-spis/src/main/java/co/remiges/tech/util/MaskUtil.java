package co.remiges.tech.util;

/**
 * Masks personal data before it is written to the server log.
 */
public final class MaskUtil {

	private MaskUtil() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Keeps the last 3 characters of a phone number, e.g. 9876543210 becomes *******210.
	 */
	public static String maskPhone(String phone) {
		if (phone == null || phone.isEmpty()) {
			return "";
		}
		int keep = Math.min(3, phone.length());
		StringBuilder masked = new StringBuilder();
		for (int i = 0; i < phone.length() - keep; i++) {
			masked.append('*');
		}
		return masked.append(phone.substring(phone.length() - keep)).toString();
	}
}
