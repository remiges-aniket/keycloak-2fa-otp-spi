package co.remiges.tech.emailservice;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import co.remiges.tech.Constants;

import java.util.*;

public class CustomResetCredentialEmailFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

    public static final String PROVIDER_ID = "reset-credential-email-custom";
    private static final CustomResetCredentialEmail SINGLETON = new CustomResetCredentialEmail();

    @Override
    public String getDisplayType() {
        return "Send Custom Reset Email";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
        AuthenticationExecutionModel.Requirement.REQUIRED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Sends the password-reset link by email through an HTTP email API such as Netcore instead of the realm SMTP settings.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(
            new ProviderConfigProperty(Constants.CFG_BRANDING_NAME, "Branding: Name", "Organisation or product name that users see in the reset email. It replaces {brandingName} in the texts below. e.g. Nuvama RTA. Leave blank to use the realm display name.", ProviderConfigProperty.STRING_TYPE, ""),
            new ProviderConfigProperty("netcoreApiUrl", "Email API: URL", "Full URL of your email provider's mail-send API. It must match 'Email API: Format'. Netcore v5: https://api.pepipost.com/v5/mail/send. Netcore v6: https://emailapi.netcorecloud.net/v6/mail/send.", ProviderConfigProperty.STRING_TYPE, ""),
            new ProviderConfigProperty("netcoreApiKey", "Email API: Key", "API key from your email provider, e.g. Netcore dashboard > Settings > Integration. It is stored masked. v5 sends it in the api_key header, v6 as 'Authorization: Bearer <key>'.", ProviderConfigProperty.PASSWORD, ""),
            EmailSender.apiFormatProperty(),
            new ProviderConfigProperty(Constants.CFG_RESET_EMAIL_FROM_NAME, "Reset Email: Sender Name", "Name shown as the sender of the reset email, e.g. Nuvama RTA. Leave blank to use Branding: Name.", ProviderConfigProperty.STRING_TYPE, ""),
            new ProviderConfigProperty(Constants.CFG_RESET_EMAIL_FROM_ADDRESS, "Reset Email: Sender Address", "Address the reset email is sent from, e.g. RTA@nuvama.com. It must belong to a domain verified with your email provider. Required: no reset email is sent while this is empty.", ProviderConfigProperty.STRING_TYPE, ""),
            new ProviderConfigProperty(Constants.CFG_RESET_EMAIL_TAGS, "Reset Email: Provider Tag", "Optional tag added to every reset email so you can find them in the provider's reports, e.g. rta-password-reset. Leave blank to use the sender name.", ProviderConfigProperty.STRING_TYPE, ""),
            new ProviderConfigProperty(Constants.CFG_RESET_EMAIL_SUBJECT_TEMPLATE, "Reset Email: Subject", "Subject line of the reset email. You can use {brandingName}, {firstName}, {lastName}, {username}. e.g. Reset Credentials for {brandingName}", ProviderConfigProperty.STRING_TYPE, Constants.DEFAULT_RESET_EMAIL_SUBJECT_TEMPLATE),
            new ProviderConfigProperty(Constants.CFG_RESET_EMAIL_BODY_TEMPLATE, "Reset Email: Body", "HTML body of the reset email. Placeholders: {link} the reset link, {brandingName}, {firstName}, {lastName}, {username}. Keep {link}, without it the user cannot reset the password. e.g. <p>Dear {username},</p><p><a href=\"{link}\">Reset your {brandingName} password</a></p>", ProviderConfigProperty.TEXT_TYPE, Constants.DEFAULT_RESET_EMAIL_BODY_TEMPLATE),
            new ProviderConfigProperty(Constants.CFG_MSG_RESET_EMAIL_SENT, "Message: Reset Email Sent", "Shown to the user after they ask for a reset. It is also shown for unknown users, so the screen does not reveal which users exist. e.g. Password reset mail sent to your registered email id", ProviderConfigProperty.STRING_TYPE, Constants.DEFAULT_MSG_RESET_EMAIL_SENT),
            new ProviderConfigProperty(Constants.CFG_USER_EMAIL_ATTRIBUTE, "User Attribute: Email", "Name of the user attribute that holds the email address, e.g. email.", ProviderConfigProperty.STRING_TYPE, Constants.DEFAULT_USER_EMAIL_ATTRIBUTE),
            new ProviderConfigProperty("isProxyRequired", "Network: Use Proxy", "Turn on if the server can only reach the email provider through an HTTP proxy.", ProviderConfigProperty.BOOLEAN_TYPE, "false"),
            new ProviderConfigProperty("proxyHost", "Network: Proxy Host", "Host name or IP of the proxy, e.g. proxy.nuvama.local. Only used when Network: Use Proxy is on.", ProviderConfigProperty.STRING_TYPE, ""),
            new ProviderConfigProperty("proxyPort", "Network: Proxy Port", "Port of the proxy, e.g. 8080. Only used when Network: Use Proxy is on.", ProviderConfigProperty.STRING_TYPE, ""),
            new ProviderConfigProperty("skiptls", "Network: Skip TLS Verification", "Do not check the certificate of the email server. Insecure: the reset link can be intercepted. Use only for testing with self-signed certificates.", ProviderConfigProperty.BOOLEAN_TYPE, false)
        );
    }


    @Override
    public void close() {

    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}