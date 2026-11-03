package co.remiges.tech.smsservice;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import co.remiges.tech.Constants;
import co.remiges.tech.emailservice.EmailSender;

/**
 * Factory class for SMS OTP send SPI
 * @author bhavyag
 */
public class SmsAuthenticatorFactory implements AuthenticatorFactory{

	private static final String PROVIDER_ID = "sms-authenticator";

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public String getDisplayType() {
		return "SMS Authentication";
	}

	@Override
	public String getHelpText() {
		return "Sends a one-time code to the user by SMS and email and checks it. The email goes through an HTTP email API such as Netcore, the SMS through an SMS gateway.";
	}

	@Override
	public String getReferenceCategory() {
		return "otp";
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
			new ProviderConfigProperty(Constants.CFG_BRANDING_NAME, "Branding: Name", "Organisation or product name that users see in the OTP SMS and email. It replaces {brandingName} in the texts below. e.g. Nuvama RTA. Leave blank to use the realm display name.", ProviderConfigProperty.STRING_TYPE, ""),
			new ProviderConfigProperty("netcoreApiUrl", "Email API: URL", "Full URL of your email provider's mail-send API. It must match 'Email API: Format'. Netcore v5: https://api.pepipost.com/v5/mail/send. Netcore v6: https://emailapi.netcorecloud.net/v6/mail/send.", ProviderConfigProperty.STRING_TYPE, ""),
			new ProviderConfigProperty("netcoreApiKey", "Email API: Key", "API key from your email provider, e.g. Netcore dashboard > Settings > Integration. It is stored masked. v5 sends it in the api_key header, v6 as 'Authorization: Bearer <key>'.", ProviderConfigProperty.PASSWORD, ""),
			EmailSender.apiFormatProperty(),
			new ProviderConfigProperty(Constants.CFG_OTP_EMAIL_FROM_NAME, "OTP Email: Sender Name", "Name shown as the sender of the OTP email, e.g. Nuvama RTA. Leave blank to use Branding: Name.", ProviderConfigProperty.STRING_TYPE, ""),
			new ProviderConfigProperty(Constants.CFG_OTP_EMAIL_FROM_ADDRESS, "OTP Email: Sender Address", "Address the OTP email is sent from, e.g. RTA@nuvama.com. It must belong to a domain verified with your email provider. Required: no OTP email is sent while this is empty.", ProviderConfigProperty.STRING_TYPE, ""),
			new ProviderConfigProperty(Constants.CFG_OTP_EMAIL_TAGS, "OTP Email: Provider Tag", "Optional tag added to every OTP email so you can find them in the provider's reports, e.g. rta-login-otp. Leave blank to use the sender name.", ProviderConfigProperty.STRING_TYPE, ""),
			new ProviderConfigProperty(Constants.CFG_OTP_EMAIL_SUBJECT_TEMPLATE, "OTP Email: Subject", "Subject line of the OTP email. You can use {brandingName}, {firstName}, {lastName}, {username}. e.g. OTP for Login to {brandingName}", ProviderConfigProperty.STRING_TYPE, Constants.DEFAULT_OTP_EMAIL_SUBJECT_TEMPLATE),
			new ProviderConfigProperty(Constants.CFG_OTP_EMAIL_BODY_TEMPLATE, "OTP Email: Body", "Body of the OTP email, HTML is allowed. Placeholders: {code} the OTP, {brandingName}, {firstName}, {lastName}, {username}, {ttlMinutes} minutes the code stays valid. e.g. <p>Your {brandingName} OTP is <b>{code}</b>. It is valid for {ttlMinutes} minutes.</p>", ProviderConfigProperty.TEXT_TYPE, Constants.DEFAULT_OTP_MESSAGE_TEMPLATE),
			new ProviderConfigProperty(Constants.CFG_SMS_MESSAGE_TEMPLATE, "SMS: Message Text", "Text of the OTP SMS. Placeholders: {code} the OTP, {brandingName}, {firstName}, {lastName}, {username}, {ttlMinutes} minutes the code stays valid. If your SMS gateway uses DLT (India), this text must match the registered template, so only change what the template allows.", ProviderConfigProperty.TEXT_TYPE, Constants.DEFAULT_OTP_MESSAGE_TEMPLATE),
			new ProviderConfigProperty("smsApiUrl", "SMS Gateway: URL", "Address of the SMS gateway that receives the OTP, e.g. https://smsgw.example.com/send. The JAR calls it with ?feedid=..&text=..&to=..&username=..&senderid=..&password=... If SMS Gateway: Sender Id is empty, this value is also sent as senderid (old behaviour).", ProviderConfigProperty.STRING_TYPE, ""),
			new ProviderConfigProperty("feedid", "SMS Gateway: Feed Id", "Feed or route id given to you by the SMS gateway provider, sent as feedid. e.g. 123456", ProviderConfigProperty.STRING_TYPE, ""),
			new ProviderConfigProperty(Constants.CFG_SMS_SENDER_ID, "SMS Gateway: Sender Id", "Sender header shown on the phone, registered with the gateway, e.g. NUVAMA. Leave blank to send the SMS Gateway URL as before.", ProviderConfigProperty.STRING_TYPE, ""),
			new ProviderConfigProperty("username", "SMS Gateway: Username", "Account username given to you by the SMS gateway provider, e.g. nuvama_otp", ProviderConfigProperty.STRING_TYPE, ""),
			new ProviderConfigProperty("password", "SMS Gateway: Password", "Account password given to you by the SMS gateway provider. It is stored masked and never written to the log.", ProviderConfigProperty.PASSWORD, ""),
			new ProviderConfigProperty("length", "OTP: Code Length (digits)", "Number of digits in the OTP, e.g. 6. A value below 4 is raised to 4.", ProviderConfigProperty.STRING_TYPE, 6),
			new ProviderConfigProperty("ttl", "OTP: Valid For (seconds)", "How long a code can be used, in seconds, e.g. 300 for 5 minutes.", ProviderConfigProperty.STRING_TYPE, "300"),
			new ProviderConfigProperty("validateOtpLimit", "OTP: Max Wrong Attempts", "How many wrong codes a user may enter before being stopped, e.g. 3. Asking for a new code starts the count again.", ProviderConfigProperty.STRING_TYPE, 3),
			new ProviderConfigProperty("resendOtpLimit", "OTP: Max Resends", "How many times a user may ask for a new code, e.g. 3. After that they must wait the time set below.", ProviderConfigProperty.STRING_TYPE, 3),
			new ProviderConfigProperty("retryAfterTime", "OTP: Wait After Max Resends (minutes)", "How long a user must wait after using all resends before asking again, in minutes, e.g. 30.", ProviderConfigProperty.STRING_TYPE, 30),
			new ProviderConfigProperty(Constants.CFG_MSG_OTP_SENT_TO_EMAIL, "Message: OTP Sent To Email", "Line shown on the OTP screen after the email is sent. Placeholder: {maskedEmail} the email with its middle hidden. e.g. Email sent to {maskedEmail}.", ProviderConfigProperty.STRING_TYPE, Constants.DEFAULT_MSG_OTP_SENT_TO_EMAIL),
			new ProviderConfigProperty(Constants.CFG_MSG_OTP_SENT_TO_MOBILE, "Message: OTP Sent To Mobile", "Line shown on the OTP screen when the user has a mobile number, e.g. OTP also sent to your registered mobile number.", ProviderConfigProperty.STRING_TYPE, Constants.DEFAULT_MSG_OTP_SENT_TO_MOBILE),
			new ProviderConfigProperty(Constants.CFG_MSG_NO_MOBILE_FOUND, "Message: No Mobile Number", "Line shown on the OTP screen when the user has no mobile number, e.g. No mobile number found.", ProviderConfigProperty.STRING_TYPE, Constants.DEFAULT_MSG_NO_MOBILE_FOUND),
			new ProviderConfigProperty(Constants.CFG_MSG_RESEND_LIMIT_EXHAUSTED, "Message: Resend Limit Reached", "Shown when the user used all resends. Placeholder: {retryMinutes} the wait time. e.g. Resend otp attempts exhausted. Try after {retryMinutes} minutes.", ProviderConfigProperty.STRING_TYPE, Constants.DEFAULT_MSG_RESEND_LIMIT_EXHAUSTED),
			new ProviderConfigProperty(Constants.CFG_USER_MOBILE_ATTRIBUTE, "User Attribute: Mobile Number", "Name of the user attribute (Users > the user > Attributes) that holds the mobile number, e.g. mobileNumber or mobile_number. Users without it get the OTP by email only.", ProviderConfigProperty.STRING_TYPE, Constants.ATTRIB_MOB_NUM),
			new ProviderConfigProperty(Constants.CFG_USER_EMAIL_ATTRIBUTE, "User Attribute: Email", "Name of the user attribute that holds the email address, e.g. email.", ProviderConfigProperty.STRING_TYPE, Constants.DEFAULT_USER_EMAIL_ATTRIBUTE),
			new ProviderConfigProperty(Constants.ATTRIB_2FA_COOKIE, "Remember Device: Enable", "If on, a user who ticks Remember Me skips the OTP on that browser for the days set below. This code uses classes from Keycloak 22, so keep it off on Keycloak 26.", ProviderConfigProperty.BOOLEAN_TYPE, false),
			new ProviderConfigProperty("cookieMaxAge", "Remember Device: Days", "How many days a remembered device skips the OTP, e.g. 7.", ProviderConfigProperty.STRING_TYPE, "7"),
			new ProviderConfigProperty("isProxyRequired", "Network: Use Proxy", "Turn on if the server can only reach the email and SMS providers through an HTTP proxy.", ProviderConfigProperty.BOOLEAN_TYPE, "false"),
			new ProviderConfigProperty("proxyHost", "Network: Proxy Host", "Host name or IP of the proxy, e.g. proxy.nuvama.local. Only used when Network: Use Proxy is on.", ProviderConfigProperty.STRING_TYPE, ""),
			new ProviderConfigProperty("proxyPort", "Network: Proxy Port", "Port of the proxy, e.g. 8080. Only used when Network: Use Proxy is on.", ProviderConfigProperty.STRING_TYPE, ""),
			new ProviderConfigProperty("skiptls", "Network: Skip TLS Verification", "Do not check the certificate of the email and SMS servers. Insecure: the OTP and the gateway password can be intercepted. Use only for testing with self-signed certificates.", ProviderConfigProperty.BOOLEAN_TYPE, false),
			new ProviderConfigProperty(Constants.CFG_SIMULATION, "Testing: Simulation Mode", "If on, no OTP is sent by SMS or by email, only the masked phone number and email are logged. The OTP is never logged, so set Testing: Fixed OTP to be able to log in. Keep off in production.", ProviderConfigProperty.BOOLEAN_TYPE, false),
			new ProviderConfigProperty(Constants.CFG_SIMULATION_OTP, "Testing: Fixed OTP", "Only used when Testing: Simulation Mode is on. Every user gets this code instead of a random one, e.g. 123456, so automated tests can log in. Digits only, at least 4. Leave blank to keep random codes. Anyone who knows it can pass the OTP step, so never use it in production.", ProviderConfigProperty.PASSWORD, "")
		);
	}

	@Override
	public Authenticator create(KeycloakSession session) {
		return new SmsAuthenticator();
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
