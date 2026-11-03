package co.remiges.tech.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

/**
 * Renders admin-configured message templates such as
 * "{code} is the OTP to access {brandingName} platform".
 *
 * Placeholders are written as {name}. A placeholder with no value in the map is
 * left as it is and a warning is logged, so a typo in the admin console never
 * fails a login.
 */
public final class TemplateUtil {

	private static final Logger LOG = Logger.getLogger(TemplateUtil.class);

	private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z][A-Za-z0-9]*)\\}");

	private TemplateUtil() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * @param template template text, may be null or blank
	 * @param values   placeholder name to replacement value; a null value is rendered as empty text
	 * @return the rendered text, or an empty string when the template is null
	 */
	public static String render(String template, Map<String, String> values) {
		return render(template, values, false);
	}

	/**
	 * Same as render, but every value is HTML-escaped before it is inserted. Use it for
	 * HTML email bodies, so a user name such as {@code <a href=evil>} cannot add markup.
	 * The template itself is written by the admin and is not escaped.
	 */
	public static String renderHtml(String template, Map<String, String> values) {
		return render(template, values, true);
	}

	public static String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	private static String render(String template, Map<String, String> values, boolean escape) {
		if (template == null) {
			return "";
		}
		Matcher matcher = PLACEHOLDER.matcher(template);
		StringBuffer out = new StringBuffer();
		while (matcher.find()) {
			String key = matcher.group(1);
			String replacement;
			if (values != null && values.containsKey(key)) {
				String value = values.get(key);
				replacement = escape ? escapeHtml(value) : (value == null ? "" : value);
			} else {
				LOG.warnf("Unknown placeholder {%s} in message template, leaving it as it is", key);
				replacement = matcher.group();
			}
			matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(out);
		return out.toString();
	}
}
