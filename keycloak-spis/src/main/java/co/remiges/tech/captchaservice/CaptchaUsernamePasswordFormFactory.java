package co.remiges.tech.captchaservice;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
//import org.keycloak.authentication.authenticators.console.ConsoleUsernamePasswordAuthenticator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;

import co.remiges.tech.Constants;

public class CaptchaUsernamePasswordFormFactory  implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "captcha-u-p-form";
    public static final CaptchaUsernamePasswordForm SINGLETON = new CaptchaUsernamePasswordForm();

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    public Authenticator createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return SINGLETON;
        if (!OAuth2Constants.DISPLAY.equalsIgnoreCase(displayType)) return null;
        return SINGLETON;//ConsoleUsernamePasswordAuthenticator.SINGLETON;
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


    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getReferenceCategory() {
        return PasswordCredentialModel.TYPE;
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
    public String getDisplayType() {
        return "Captcha Username Password Form";
    }

    @Override
    public String getHelpText() {
        return "Shows the login form with a captcha and checks it, then validates the username and password. The password can arrive encrypted by the login page.";
    }

	private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(Constants.VALIDATE_CAPTCHA_API);
        property.setLabel("Captcha API: Validation URL");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("URL of the service that checks the captcha the user typed. The login sends it the captcha text and captcha id. The service must answer with status success. e.g. https://rta.example.com/validate_captcha");
        CONFIG_PROPERTIES.add(property);
        
  
        property = new ProviderConfigProperty();
        property.setName(Constants.INCORRECT_CAPTCHA_MSG);
        property.setLabel("Message: Wrong Captcha");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Text shown on the login page when the captcha is wrong or missing. e.g. Incorrect captcha, please try again");
        CONFIG_PROPERTIES.add(property);

        property = new ProviderConfigProperty();
        property.setName(Constants.CFG_CAPTCHA_ENCRYPTION_SECRET);
        property.setLabel("Login Page: Credentials Encryption Secret");
        property.setType(ProviderConfigProperty.PASSWORD);
        property.setHelpText("Secret the login page script uses to encrypt the username and password before sending them. It must be exactly the same value as in that script, e.g. a long random string such as 7f3c9a1e5b2d4c88. If blank, the old built-in secret is used and a warning is logged.");
        CONFIG_PROPERTIES.add(property);
     
    }

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return CONFIG_PROPERTIES;
	}

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

}
