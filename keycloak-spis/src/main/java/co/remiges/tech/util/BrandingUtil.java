package co.remiges.tech.util;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.RealmModel;

import co.remiges.tech.Constants;

/**
 * Resolves the branding name that fills {brandingName} in message templates.
 */
public final class BrandingUtil {

	private BrandingUtil() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Copy of the authenticator config where a blank brandingName is replaced by the
	 * realm display name, or the realm name when there is no display name.
	 */
	public static Map<String, String> withBranding(Map<String, String> config, RealmModel realm) {
		Map<String, String> cfg = config == null ? new HashMap<>() : new HashMap<>(config);
		if (ConfigUtil.isBlank(cfg.get(Constants.CFG_BRANDING_NAME))) {
			String realmBrand = ConfigUtil.isBlank(realm.getDisplayName()) ? realm.getName() : realm.getDisplayName();
			cfg.put(Constants.CFG_BRANDING_NAME, realmBrand);
		}
		return cfg;
	}
}
