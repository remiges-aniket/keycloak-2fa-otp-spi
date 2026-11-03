package co.remiges.tech.emailservice;

import java.util.List;
import java.util.Map;

import org.keycloak.provider.ProviderConfigProperty;

import co.remiges.tech.Constants;
import co.remiges.tech.util.ConfigUtil;

/**
 * Sender details of an email sent through Netcore, read from the authenticator config.
 */
public class EmailSender {

    private final String fromName;
    private final String fromAddress;
    private final String tags;
    private final String apiFormat;

    public EmailSender(String fromName, String fromAddress, String tags, String apiFormat) {
        this.fromName = fromName;
        this.fromAddress = fromAddress;
        this.tags = tags;
        this.apiFormat = apiFormat;
    }

    /**
     * Config field that picks the JSON shape sent to the Netcore email API.
     */
    public static ProviderConfigProperty apiFormatProperty() {
        ProviderConfigProperty property = new ProviderConfigProperty(Constants.CFG_EMAIL_API_FORMAT, "Email API: Format",
                "Must match the version of 'Email API: URL'. v5: URL ends in /v5/mail/send, key sent in the api_key header. v6: URL ends in /v6/mail/send, key sent as 'Authorization: Bearer', mail marked as OTP priority. legacy: the older BSE-style gateway request, key in the api_key header. Pick v5 for https://api.pepipost.com/v5/mail/send.",
                ProviderConfigProperty.LIST_TYPE, Constants.EMAIL_API_FORMAT_LEGACY);
        property.setOptions(List.of(Constants.EMAIL_API_FORMAT_LEGACY, Constants.EMAIL_API_FORMAT_V5, Constants.EMAIL_API_FORMAT_V6));
        return property;
    }

    /**
     * @param config         authenticator config map
     * @param fromNameKey    config key of the sender name; falls back to brandingName
     * @param fromAddressKey config key of the sender address; there is no fallback
     * @param tagsKey        config key of the tags; falls back to the sender name
     */
    public static EmailSender fromConfig(Map<String, String> config, String fromNameKey, String fromAddressKey,
            String tagsKey) {
        String fromName = ConfigUtil.getFirst(config, "", fromNameKey, Constants.CFG_BRANDING_NAME);
        String fromAddress = ConfigUtil.get(config, fromAddressKey, "");
        String tags = ConfigUtil.get(config, tagsKey, fromName);
        String apiFormat = ConfigUtil.get(config, Constants.CFG_EMAIL_API_FORMAT, Constants.EMAIL_API_FORMAT_LEGACY)
                .trim().toLowerCase();
        return new EmailSender(fromName, fromAddress, tags, apiFormat);
    }

    public String getFromName() {
        return fromName;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public String getTags() {
        return tags;
    }

    /** v5 and v6 use the same request body, the legacy format uses the older one. */
    public boolean isV5() {
        return Constants.EMAIL_API_FORMAT_V5.equals(apiFormat) || isV6();
    }

    public boolean isV6() {
        return Constants.EMAIL_API_FORMAT_V6.equals(apiFormat);
    }
}
