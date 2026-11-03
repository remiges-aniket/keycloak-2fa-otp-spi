package co.remiges.tech;

/**
 * Keycloak SPI Constants
 * @author bhavyag
 */
public class Constants {

	private Constants() {
	}
	
	/**
	 * Role to be referred to Client Level attributes for IP Validation
	 */
	public static final String ROLE_IP_VALIDATION="IPWhiteListRole";
	
	/**
	 * Attribute name for IP Whitelist. This attribute name will be searched for in Keycloak Configuration at User or Client level
	 */
	public static final String ATTRIB_IP_WHITELIST="ValidIpWhitelist";
	
	/**
	 * Attribute name for Valid ISO Geo Location. This attribute name will be searched for in Keycloak Configuration at User or Client level
	 */
	public static final String ATTRIB_IP_GEO_LOC="ValidISOGeoLocation";
	
	/**
	 * Attribute name for Mobile number for SMS OTP.  This attribute name will be searched for in Keycloak Configuration at User level
	 */
	public static final String ATTRIB_MOB_NUM="mobileNumber";
	/**
	 * Cookie name if 2FA is answered
	 */
	public static final String COOKIE_2FA_ANSWERED="COOKIE_2FA_ANSWERED";
	/**
	 * Cookie name if 2FA is answered
	 */
	public static final String ATTRIB_2FA_COOKIE="setCookieFor2FA";
	/**
	 * validate captcha rest api
	 */
	public static final String VALIDATE_CAPTCHA_API = "validateCaptchaAPI";
	/**
	 * invalid captcha msg
	 */
	public static final String INCORRECT_CAPTCHA_MSG = "incorrectCaptchaMsg";
	/**
	 * validate captcha api header key
	 */
	public static final String HEADER_KEY = "headerKey";
	/**
	 * validate user rest api
	 */
	public static final String VALIDATE_USER_API = "validateUserAPI";
	/**
	 * invalid user msg
	 */
	public static final String INCORRECT_USER_MSG = "incorrectUserMsg";

	/**
	 * Shared brand name, replaces {brandingName} in all message templates
	 */
	public static final String CFG_BRANDING_NAME = "brandingName";

	/**
	 * OTP SMS config keys
	 */
	public static final String CFG_SMS_MESSAGE_TEMPLATE = "smsMessageTemplate";
	public static final String CFG_SMS_SENDER_ID = "smsSenderId";

	/**
	 * Fixed OTP used instead of a random one, only while simulation mode is on
	 */
	public static final String CFG_SIMULATION = "simulation";
	public static final String CFG_SIMULATION_OTP = "simulationOtp";

	/**
	 * OTP email config keys
	 */
	public static final String CFG_OTP_EMAIL_FROM_NAME = "otpEmailFromName";
	public static final String CFG_OTP_EMAIL_FROM_ADDRESS = "otpEmailFromAddress";
	public static final String CFG_OTP_EMAIL_TAGS = "otpEmailTags";
	public static final String CFG_OTP_EMAIL_SUBJECT_TEMPLATE = "otpEmailSubjectTemplate";
	public static final String CFG_OTP_EMAIL_BODY_TEMPLATE = "otpEmailBodyTemplate";

	/**
	 * Reset password email config keys
	 */
	public static final String CFG_EMAIL_API_FORMAT = "emailApiFormat";
	public static final String EMAIL_API_FORMAT_LEGACY = "legacy";
	public static final String EMAIL_API_FORMAT_V5 = "v5";
	public static final String EMAIL_API_FORMAT_V6 = "v6";

	public static final String CFG_RESET_EMAIL_FROM_NAME = "resetEmailFromName";
	public static final String CFG_RESET_EMAIL_FROM_ADDRESS = "resetEmailFromAddress";
	public static final String CFG_RESET_EMAIL_TAGS = "resetEmailTags";
	public static final String CFG_RESET_EMAIL_SUBJECT_TEMPLATE = "resetEmailSubjectTemplate";
	public static final String CFG_RESET_EMAIL_BODY_TEMPLATE = "resetEmailBodyTemplate";

	/**
	 * On-screen message config keys
	 */
	public static final String CFG_MSG_OTP_SENT = "messageOtpSent";
	public static final String CFG_MSG_OTP_SENT_TO_EMAIL = "messageOtpSentToEmail";
	public static final String CFG_MSG_OTP_SENT_TO_MOBILE = "messageOtpSentToMobile";
	public static final String CFG_MSG_NO_MOBILE_FOUND = "messageNoMobileFound";
	public static final String CFG_MSG_RESEND_LIMIT_EXHAUSTED = "messageResendLimitExhausted";
	public static final String CFG_MSG_RESET_EMAIL_SENT = "messageResetEmailSent";
	public static final String CFG_MSG_IP_NOT_WHITELISTED = "messageIpNotWhitelisted";
	public static final String CFG_MSG_GEO_NOT_ALLOWED = "messageGeoNotAllowed";
	public static final String CFG_MSG_GEO_DB_ERROR = "messageGeoDbError";

	/**
	 * User attribute name config keys
	 */
	public static final String CFG_USER_MOBILE_ATTRIBUTE = "userMobileAttribute";
	public static final String CFG_USER_EMAIL_ATTRIBUTE = "userEmailAttribute";

	/**
	 * Secret used to decrypt the captcha payload
	 */
	public static final String CFG_CAPTCHA_ENCRYPTION_SECRET = "captchaEncryptionSecret";

	/**
	 * Default OTP message texts, used when the config field is blank. The spacing of the
	 * SMS text is kept as it was, because SMS gateways in India match it against a registered DLT template.
	 */
	public static final String DEFAULT_OTP_MESSAGE_TEMPLATE = "{code} is the OTP to access {brandingName} platform for user:  {firstName} {lastName} username: {username}. OTP valid for {ttlMinutes} Minutes . Pls do not share it with anyone.";
	public static final String DEFAULT_OTP_EMAIL_SUBJECT_TEMPLATE = "OTP for Login to {brandingName}";
	public static final String DEFAULT_MSG_OTP_SENT_TO_EMAIL = "Email sent to {maskedEmail}.";
	public static final String DEFAULT_MSG_OTP_SENT_TO_MOBILE = "OTP also sent to your registered mobile number.";
	public static final String DEFAULT_MSG_NO_MOBILE_FOUND = "No mobile number found.";
	public static final String DEFAULT_MSG_RESEND_LIMIT_EXHAUSTED = "Resend otp attempts exhausted. Try after {retryMinutes} minutes.";
	public static final String DEFAULT_USER_EMAIL_ATTRIBUTE = "email";
	public static final String DEFAULT_MSG_IP_NOT_WHITELISTED = "Your IP Address <{ip}> is not part of whitelist, therefore access is denied";
	public static final String DEFAULT_MSG_GEO_NOT_ALLOWED = "Your IP Address <{ip}> belongs to <{country}> is not part of whitelisted Geo Location<{allowedCountry}> for you, therefore access is denied";
	public static final String DEFAULT_MSG_GEO_DB_ERROR = "GeoIP Database Error for your IP <{ip}>";
	public static final String DEFAULT_RESET_EMAIL_SUBJECT_TEMPLATE = "Reset Credentials for {brandingName}";
	public static final String DEFAULT_RESET_EMAIL_BODY_TEMPLATE = "<html><body><p>Dear {username},</p><p>Please click on the link below to reset your password:</p><a href='{link}' style='padding: 10px; background-color: #007BFF; color: white; text-decoration: none; border-radius: 5px;'>Reset Password</a><p>If you did not request a password reset, please ignore this email.</p></body></html>";
	public static final String DEFAULT_MSG_RESET_EMAIL_SENT = "Password reset mail sent to your registered email id";

	/**
	 * Placeholder names usable in message templates, written as {name}
	 */
	public static final String PH_BRANDING_NAME = "brandingName";
	public static final String PH_CODE = "code";
	public static final String PH_FIRST_NAME = "firstName";
	public static final String PH_LAST_NAME = "lastName";
	public static final String PH_USERNAME = "username";
	public static final String PH_TTL_MINUTES = "ttlMinutes";
	public static final String PH_LINK = "link";
	public static final String PH_RETRY_MINUTES = "retryMinutes";
	public static final String PH_MASKED_EMAIL = "maskedEmail";
	public static final String PH_IP = "ip";
	public static final String PH_COUNTRY = "country";
	public static final String PH_ALLOWED_COUNTRY = "allowedCountry";
}
