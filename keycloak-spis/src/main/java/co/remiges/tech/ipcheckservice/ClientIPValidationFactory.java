package co.remiges.tech.ipcheckservice;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import co.remiges.tech.Constants;
import co.remiges.tech.ipcheckservice.ClientIPValidator;

/**
 * Client IP Validation Factory which implements Keycloak Authentication Factory.
 * This is used for IP validation SPI
 * @author bhavyag
 */
public class ClientIPValidationFactory implements AuthenticatorFactory {

	private static final String PROVIDER_ID = "ip-validator";
	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public String getDisplayType() {
		return "IP Validator";
	}

	@Override
	public String getHelpText() {
		return "Allows login only from whitelisted IP addresses and, optionally, only from an allowed country.";
	}

	@Override
	public String getReferenceCategory() {
		return "IPValidator";
	}

	@Override
	public boolean isConfigurable() {
		return true;
	}

	@Override
	public boolean isUserSetupAllowed() {
		return true;
	}

	@Override
	public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
		return REQUIREMENT_CHOICES;
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return List.of(
			new ProviderConfigProperty("IP Validation", "Check IP Whitelist", "If on, the user's IP must be in the whitelist. The whitelist is the user or client attribute ValidIpWhitelist, e.g. 10.0.0.1, 10.0.0.5-10.0.0.9, 2001:db8::1 or *.*.*.* for any IP. If the attribute is empty nobody is blocked. If off, the whitelist is skipped.", ProviderConfigProperty.BOOLEAN_TYPE, true),
			new ProviderConfigProperty("Geo IP Validation", "Check Country (Geo IP)", "If on, the country of the user's IP must match the user or client attribute ValidISOGeoLocation, a 2-letter ISO code such as IN. If the attribute is empty nobody is blocked. If off, the country check is skipped.", ProviderConfigProperty.BOOLEAN_TYPE, true),
			new ProviderConfigProperty(Constants.CFG_MSG_IP_NOT_WHITELISTED, "Message: IP Not Whitelisted", "Shown when the IP is not in the whitelist. Placeholder: {ip} the user IP. e.g. Your IP Address <{ip}> is not allowed to log in.", ProviderConfigProperty.STRING_TYPE, Constants.DEFAULT_MSG_IP_NOT_WHITELISTED),
			new ProviderConfigProperty(Constants.CFG_MSG_GEO_NOT_ALLOWED, "Message: Country Not Allowed", "Shown when the country of the IP is not the allowed one. Placeholders: {ip}, {country} found for the IP, {allowedCountry} the allowed one. e.g. Logins from {country} are not allowed, only from {allowedCountry}.", ProviderConfigProperty.STRING_TYPE, Constants.DEFAULT_MSG_GEO_NOT_ALLOWED),
			new ProviderConfigProperty(Constants.CFG_MSG_GEO_DB_ERROR, "Message: Geo IP Database Error", "Shown when the country of the IP cannot be found, for example a private IP or a missing GeoLite2 database. Placeholder: {ip}. e.g. We could not check the country of your IP <{ip}>.", ProviderConfigProperty.STRING_TYPE, Constants.DEFAULT_MSG_GEO_DB_ERROR)
		);
	}

	@Override
	public Authenticator create(KeycloakSession session) {
		return new ClientIPValidator();
	}

	@Override
	public void init(Config.Scope config) {
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
	}

	@Override
	public void close() {
	}
}
