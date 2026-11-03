package co.remiges.tech.smsservice;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.TokenCategory;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.ServerCookie;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.sessions.AuthenticationSessionModel;
import co.remiges.tech.Constants;
import co.remiges.tech.emailservice.EmailSender;
import co.remiges.tech.emailservice.EmailService;
import co.remiges.tech.emailservice.EmailService.EmailResponse;
import co.remiges.tech.smsservice.smsgateway.SmsServiceFactory;
import co.remiges.tech.util.BrandingUtil;
import co.remiges.tech.util.ConfigUtil;
import co.remiges.tech.util.TemplateUtil;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.time.LocalDateTime;

/**
 * This SPI can be used to send SMS OTP for Login Authentication using Keycloak.
 * To enable SMS OTP as a step in Authentication Workflow, 1. Login to Keycloak
 * Admin Portal. 2. Click on the relevant Realm on top Left corner. 3. Click on
 * "Authentication" in "configure" section on left sidebar. 4. we'll create a
 * copy of existing "Browser" workflow for our use. a. Click on "Browser"
 * authentication flow b. on top right, there is a drop down named 'Action',
 * select duplicate from it. This will create a duplicate browser authentication
 * flow c. Name this new workflow to example 'browserWithSMSOtp' d. In
 * 'browserWithSMSOtp forms' section, add a sub-section by clicking on '+'
 * symbol besides the step and click on 'Add step' e. In the list search for
 * 'SMS Authentication'. f. Add an alias to this setting, and set the parameters
 * 'Code length' i.e. SMS OTP length 'Time-to-live' i.e. TTL for which OTP is
 * valid 'SenderId' i.e. What should be the SenderId in the SMS OTP 'Simulation
 * mode' i.e. if on, it'll not actually send the OTP instead just print OTP in
 * console g. Click on save h. Now set the flow to be used for browser. So click
 * on drop down 'Action' on top right and click on 'Bind Flow' and select
 * 'Browser flow' and click on 'Save'
 * 
 * @author bhavyag
 *
 */
public class SmsAuthenticator implements Authenticator {

	private static final String TPL_CODE = "login-sms.ftl";
	private static final Logger LOG = Logger.getLogger(SmsAuthenticator.class);
	private static final String CONST_REALM = "realm";
	private EmailService emailService;
	private static final String OTP_SENT = "otp sent successfully";
	private static final String GENERIC_ERROR_DETAIL = "a server error";
	/** An OTP shorter than this is too easy to guess, so a smaller configured length is raised to it. */
	private static final int MIN_OTP_LENGTH = 4;

	public SmsAuthenticator() {
		this.emailService = new EmailService(); // Initialize the email service
	}

	@Override
	public void authenticate(AuthenticationFlowContext context) {
		AuthenticatorConfigModel config = context.getAuthenticatorConfig();
		UserModel user = context.getUser();
		MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
		String mobileNumber = "";

		/**
		 * Fetch mobile no from User attribute of Keycloak
		 */
		String mobileAttribute = ConfigUtil.get(config.getConfig(), Constants.CFG_USER_MOBILE_ATTRIBUTE,
				Constants.ATTRIB_MOB_NUM);
		mobileNumber = user.getFirstAttribute(mobileAttribute);
		if (mobileNumber == null || mobileNumber.isBlank()) {
			String errorString = String.format(
					"'%s' attribute for the user <%s>is blank or not present. Please set it.", mobileAttribute,
					user.getUsername());
			LOG.error(errorString);
		}

		try {
			AuthenticationSessionModel authSession = context.getAuthenticationSession();

			String rememberMe = formData.containsKey("rememberMe") ? formData.getFirst("rememberMe") : "off";
			String errorString = String.format("rememberMe:: '%s'", rememberMe);
			LOG.info(errorString);
			authSession.setAuthNote("rememberMe", rememberMe);
			if (rememberMe.equalsIgnoreCase("on")) {
				if (hasCookie(context)) {
					context.success();
					return;
				}
			}

			/*
			 * If OTP wasrememberMe = formData.containsKey("rememberMe") ?
			 * formData.getFirst("rememberMe") : "off"; already generated, and is valid,
			 * then there is no need to re-generate the OTP.
			 */
			if (authSession.getAuthNote("code") != null
					&& Long.parseLong(authSession.getAuthNote("ttl")) > System.currentTimeMillis()) {
				// show the OTP form again. Marking the step as attempted would let an alternative or
				// conditional step be skipped by reloading the login page.
				context.challenge(context.form().setAttribute(CONST_REALM, context.getRealm()).createForm(TPL_CODE));
				return;
			}
			/*
			 * length: Length of OTP to be generated. This is currently defined in the
			 * Authentication flow. Can be read from config as well.
			 */
			int length = Math.max(MIN_OTP_LENGTH, Integer.parseInt(config.getConfig().get("length")));
			/*
			 * Generating OTP
			 */
			String code = generateCode(context, length);
			/*
			 * Set OTP in current device session
			 */
			authSession.setAuthNote("code", code);
			authSession.setAuthNote("resendOtpLimit", config.getConfig().get("resendOtpLimit"));
			authSession.setAuthNote("length", config.getConfig().get("length"));
			authSession.setAuthNote("invalidOtpCount", "0");
			authSession.setAuthNote("resendOtpCount", "0");
			/*
			 * Send OTP
			 */
			sendOTP(context, code);

		} catch (NumberFormatException e) {
			LOG.error(String.format("Unable to parse length<%s> or ttl<%s> for the Authentication Flow",
					config.getConfig().get("length"), config.getConfig().get("ttl")));
			context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
					context.form().setError("smsAuthSmsNotSent", GENERIC_ERROR_DETAIL)
							.createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
		}
	}

	/**
	 * Returns the fixed test OTP when simulation mode is on and a valid one is configured,
	 * otherwise a random OTP. The fixed code is never used while simulation mode is off.
	 *
	 * @param context AuthenticationFlowContext
	 * @param length  length of a random OTP
	 * @return String OTP code
	 */
	private String generateCode(AuthenticationFlowContext context, int length) {
		Map<String, String> config = context.getAuthenticatorConfig().getConfig();
		String fixedCode = ConfigUtil.get(config, Constants.CFG_SIMULATION_OTP, "").trim();
		if (ConfigUtil.getBoolean(config, Constants.CFG_SIMULATION, false) && !fixedCode.isEmpty()) {
			if (fixedCode.length() >= MIN_OTP_LENGTH && fixedCode.chars().allMatch(Character::isDigit)) {
				LOG.warn(String.format("***** SIMULATION MODE ***** fixed test OTP used for user <%s>",
						context.getUser().getUsername()));
				return fixedCode;
			}
			LOG.warn("***** SIMULATION MODE ***** 'Testing: Fixed OTP' must be at least " + MIN_OTP_LENGTH
					+ " digits, using a random OTP");
		}
		return SecretGenerator.getInstance().randomString(length, SecretGenerator.DIGITS);
	}

	/**
	 * Method to Send OTP via both SMS and EMail
	 * 
	 * @param context AuthenticationFlowContext
	 * @param code    OTP code
	 */
	private void sendOTP(AuthenticationFlowContext context, String code) {

		AuthenticatorConfigModel config = context.getAuthenticatorConfig();
		Map<String, String> cfg = BrandingUtil.withBranding(config.getConfig(), context.getRealm());
		KeycloakSession session = context.getSession();
		UserModel user = context.getUser();
		String otpSentMessage = String.format("%s", OTP_SENT);
		EmailResponse emailResponse = null;

		/*
		 * ttl: Time to Live of OTP to be generated
		 */
		int ttl = Integer.parseInt(config.getConfig().get("ttl"));

		try {
			/*
			 * SMS OTP Send
			 */
			String mobileNumber = "";
			/**
			 * Fetch mobile no from User attribute of Keycloak
			 */

			String mobileAttribute = ConfigUtil.get(cfg, Constants.CFG_USER_MOBILE_ATTRIBUTE, Constants.ATTRIB_MOB_NUM);
			mobileNumber = user.getFirstAttribute(mobileAttribute);
			if (mobileNumber == null || mobileNumber.isBlank()) {
				String errorString = String.format(
						"'%s' attribute for the user <%s>is blank or not present. Please set it.",
						mobileAttribute, user.getUsername());
				LOG.error(errorString);
			}

			/*
			 * Values for the {placeholders} of the SMS and email templates
			 */
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put(Constants.PH_BRANDING_NAME, cfg.get(Constants.CFG_BRANDING_NAME));
			placeholders.put(Constants.PH_CODE, code);
			placeholders.put(Constants.PH_FIRST_NAME, user.getFirstName());
			placeholders.put(Constants.PH_LAST_NAME, user.getLastName());
			placeholders.put(Constants.PH_USERNAME, user.getUsername());
			placeholders.put(Constants.PH_TTL_MINUTES, String.valueOf(Math.floorDiv(ttl, 60)));

			if (mobileNumber != null) {
				String smsText = TemplateUtil.render(
						ConfigUtil.get(cfg, Constants.CFG_SMS_MESSAGE_TEMPLATE, Constants.DEFAULT_OTP_MESSAGE_TEMPLATE),
						placeholders);

				SmsServiceFactory.get(config.getConfig()).send(mobileNumber, smsText);
			}
			/*
			 * Email OTP Send
			 */
			// sendEmailWithCode(session, context.getRealm(), user, code);

			String emailAttribute = ConfigUtil.get(cfg, Constants.CFG_USER_EMAIL_ATTRIBUTE,
					Constants.DEFAULT_USER_EMAIL_ATTRIBUTE);
			String emailId = user.getFirstAttribute(emailAttribute);
			LOG.info("emailId:: " + obfuscateEmail(emailId));
			// user values such as the first name are HTML-escaped, the email body is HTML
			String mailSendMsg = TemplateUtil.renderHtml(
					ConfigUtil.get(cfg, Constants.CFG_OTP_EMAIL_BODY_TEMPLATE, Constants.DEFAULT_OTP_MESSAGE_TEMPLATE),
					placeholders);
			EmailSender emailSender = EmailSender.fromConfig(cfg, Constants.CFG_OTP_EMAIL_FROM_NAME,
					Constants.CFG_OTP_EMAIL_FROM_ADDRESS, Constants.CFG_OTP_EMAIL_TAGS);
			String apiKey = config.getConfig().get("netcoreApiKey");
			String apiUrl = config.getConfig().get("netcoreApiUrl");
			String proxyHost = config.getConfig().get("proxyHost");
			String proxyPort = config.getConfig().get("proxyPort");
			boolean skiptls = Boolean.parseBoolean(config.getConfig().get("skiptls"));
			boolean isProxyRequired = Boolean.parseBoolean(config.getConfig().get("isProxyRequired"));
			boolean simulation = ConfigUtil.getBoolean(config.getConfig(), Constants.CFG_SIMULATION, false);
			String subject = TemplateUtil.render(ConfigUtil.get(cfg, Constants.CFG_OTP_EMAIL_SUBJECT_TEMPLATE,
					Constants.DEFAULT_OTP_EMAIL_SUBJECT_TEMPLATE), placeholders);
			if (simulation) {
				// like the SMS, no OTP email goes out in simulation mode
				LOG.warn(String.format("***** SIMULATION MODE ***** Email to %s not sent", obfuscateEmail(emailId)));
			} else if ((emailId != null) && (!isProxyRequired)) {
				try {
					emailResponse = emailService.sendEmailWithoutProxy(emailId, subject, mailSendMsg, emailSender, apiKey, apiUrl);
					if (!emailResponse.isSuccess()) {
						context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
						context.form().setError("emailSendFailed").createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
						LOG.error("Failed to send email: " + emailResponse.getErrorMessage());
					}
					// else {
					// context.forkWithSuccessMessage(new FormMessage(otpSentMessage));
					// return;
					// }
				} catch (Exception e) {
					LOG.error("Failed to send email" + e.getMessage(), e);
					// Create a Response that indicates an error and pass it to context.failure
					Response response = Response.status(Response.Status.INTERNAL_SERVER_ERROR)
							.entity("Failed to send email due to server error").build();
					context.failure(AuthenticationFlowError.INTERNAL_ERROR, response);
				}
			} else if ((emailId != null) && (isProxyRequired)) {
				try {
					emailResponse = emailService.sendEmailWithProxy(emailId, subject, mailSendMsg, emailSender, apiKey, apiUrl,
							proxyHost, proxyPort, skiptls);
					if (!emailResponse.isSuccess()) {
						context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
						context.form().setError("emailSendFailed").createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
						LOG.error("Failed to send email: " + emailResponse.getErrorMessage());
					}
					// else {
					// context.forkWithSuccessMessage(new FormMessage(otpSentMessage));
					// return;
					// }
				} catch (Exception e) {
					LOG.error("Failed to send email" + e.getMessage(), e);
					// Create a Response that indicates an error and pass it to context.failure
					Response response = Response.status(Response.Status.INTERNAL_SERVER_ERROR)
							.entity("Failed to send email due to server error").build();
					context.failure(AuthenticationFlowError.INTERNAL_ERROR, response);
				}

			} else {
				// Log and handle the null or invalid email case
				LOG.error("Email ID is null or invalid for user: " + user.getUsername());
				Response response = Response.status(Response.Status.BAD_REQUEST)
						.entity("Invalid or missing email attribute").build();
				context.failure(AuthenticationFlowError.INVALID_USER, response);
			}
			String otpSendMsg = "";

			if (emailResponse != null && emailResponse.isSuccess()) {
				Map<String, String> emailMsgValues = new HashMap<>();
				emailMsgValues.put(Constants.PH_MASKED_EMAIL, obfuscateEmail(emailId));
				otpSendMsg += TemplateUtil.render(ConfigUtil.get(cfg, Constants.CFG_MSG_OTP_SENT_TO_EMAIL,
						Constants.DEFAULT_MSG_OTP_SENT_TO_EMAIL), emailMsgValues) + " ";
			}

			if (mobileNumber != null && !mobileNumber.isBlank()) {
				otpSendMsg += ConfigUtil.get(cfg, Constants.CFG_MSG_OTP_SENT_TO_MOBILE,
						Constants.DEFAULT_MSG_OTP_SENT_TO_MOBILE);
			} else {
				otpSendMsg += ConfigUtil.get(cfg, Constants.CFG_MSG_NO_MOBILE_FOUND,
						Constants.DEFAULT_MSG_NO_MOBILE_FOUND);
			}

			context.form().setAttribute("isOtpSendMsgSet", true);
			context.form().setAttribute("otpSendMsg", otpSendMsg);
			context.getAuthenticationSession().setAuthNote("ttl",
					Long.toString(System.currentTimeMillis() + (ttl * 1000L)));
			
			context.challenge(context.form().setAttribute(CONST_REALM, context.getRealm()).createForm(TPL_CODE));
			

		} catch (Exception e) {
			context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
					context.form().setError("smsAuthSmsNotSent", GENERIC_ERROR_DETAIL)
							.createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
		}
	}

	public String obfuscateEmail(String email) {
		if (email == null || !email.contains("@")) {
			// Invalid email format
			return email;
		}

		String[] parts = email.split("@");
		if (parts.length != 2 || parts[1].indexOf('.') < 1) {
			// e.g. a@localhost has no dot in the domain, so mask everything after the first character
			return email.substring(0, 1) + "***";
		}
		String localPart = parts[0];
		String domainPart = parts[1];

		// Obfuscate the local part
		String obfuscatedLocalPart = obfuscateLocalPart(localPart);
		String obfuscateDomainPart = obfuscateDomainPart(domainPart);

		// Return the obfuscated email
		return obfuscatedLocalPart + "@" + obfuscateDomainPart
				+ domainPart.substring(domainPart.indexOf("."), domainPart.length());
	}

	private String obfuscateLocalPart(String localPart) {
		int length = localPart.length();
		if (length <= 2) {
			// If the local part has 2 or fewer characters, don't obfuscate
			return localPart;
		}

		// Obfuscate all characters except the first and last two
		String prefix = localPart.substring(0, 2);
		String suffix = localPart.substring(length - 2);
		String obscuredPart = localPart.substring(2, length - 2).replaceAll(".", "*");

		return prefix + obscuredPart + suffix;
	}

	private String obfuscateDomainPart(String domainPart) {

		String obscuredPart = domainPart.substring(0, domainPart.indexOf(".")).replaceAll(".", "*");

		return obscuredPart;
	}

	@Override
	public void action(AuthenticationFlowContext context) {

		MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
		AuthenticationSessionModel authSession = context.getAuthenticationSession();
		AuthenticatorConfigModel config = context.getAuthenticatorConfig();
		int validateOtpLimitConfig = 0;
		int invalidOtpCount = 0;
		Map<String, String> retryValues = new HashMap<>();
		retryValues.put(Constants.PH_RETRY_MINUTES, config.getConfig().get("retryAfterTime"));
		String smsResendLimitExhausted = TemplateUtil.render(ConfigUtil.get(config.getConfig(),
				Constants.CFG_MSG_RESEND_LIMIT_EXHAUSTED, Constants.DEFAULT_MSG_RESEND_LIMIT_EXHAUSTED), retryValues);
		try {
			if (formData.containsKey("resend")) {

				boolean resendOtp = isResendOtpLimitExceeded(context);
				boolean tryAfter = false;

				if (resendOtp) {
					tryAfter = checkRetryAfter(context);
					if (!tryAfter) {
						context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
								context.form().setAttribute(CONST_REALM, context.getRealm())
										.setError(smsResendLimitExhausted).createForm(TPL_CODE));
						return;
					}
				}

				LOG.info("inside resend login otp");
				authSession.setAuthNote("invalidOtpCount", String.valueOf(0));

				int length = Integer.parseInt(authSession.getAuthNote("length"));
				String code = generateCode(context, length);
				sendOTP(context, code);
				authSession.setAuthNote("code", code);
				LOG.info(String.format("Resending OTP for user <%s>", context.getUser().getUsername()));
				context.challenge(context.form().setAttribute(CONST_REALM, context.getRealm()).createForm(TPL_CODE));
				return;
			}

			validateOtpLimitConfig = Integer.parseInt(config.getConfig().get("validateOtpLimit"));
			invalidOtpCount = Integer.parseInt(context.getAuthenticationSession().getAuthNote("invalidOtpCount"));
			if (invalidOtpCount >= validateOtpLimitConfig) {
				LOG.info("Validate otp attempts exhausted");
				context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
						context.form().setAttribute(CONST_REALM, context.getRealm()).setError("smsOtpLimitExhausted")
								.createForm(TPL_CODE));
				return;
			}

			context.form().setAttribute("isOtpSendMsgSet", false);
			context.form().setAttribute("otpSendMsg", "");

			String enteredCode = context.getHttpRequest().getDecodedFormParameters().getFirst("code");

			String code = authSession.getAuthNote("code");
			String ttl = authSession.getAuthNote("ttl");

			if (code == null || ttl == null) {
				context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
						context.form().createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
				return;
			}

			// a missing code field counts as a wrong code, and the compare takes the same time for any input
			boolean isValid = enteredCode != null && MessageDigest.isEqual(
					enteredCode.getBytes(StandardCharsets.UTF_8), code.getBytes(StandardCharsets.UTF_8));
			if (isValid) {
				if (Long.parseLong(ttl) < System.currentTimeMillis()) {
					/*
					 * OTP expired
					 */
					context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE,
							context.form().setError("smsAuthCodeExpired").createErrorPage(Response.Status.BAD_REQUEST));
				} else {
					/*
					 * OTP is valid
					 */
					// TODO change logging to debug
					String rememberMe = context.getAuthenticationSession().getAuthNote("rememberMe");
					LOG.info("OTP validation success for " + context.getUser().getUsername());
					context.success();
					if (rememberMe.equalsIgnoreCase("on")) {
						setCookie(context);
					}
					removeSessionOTP(context);
				}
			} else {
				// OTP is invalid

				int count = Integer.parseInt(context.getAuthenticationSession().getAuthNote("invalidOtpCount"));
				authSession.setAuthNote("invalidOtpCount", String.valueOf(count + 1));
				LOG.debug(String.format("set otp limit: <%s>", String.valueOf(count + 1)));

				// neither the entered nor the generated OTP is logged
				LOG.errorf("OTP validation failed for user <%s>, attempt %d", context.getUser().getUsername(),
						count + 1);
				AuthenticationExecutionModel execution = context.getExecution();
				if (execution.isRequired()) {
					context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
							context.form().setAttribute(CONST_REALM, context.getRealm()).setError("smsAuthCodeInvalid")
									.createForm(TPL_CODE));
				} else if (execution.isConditional() || execution.isAlternative()) {
					context.attempted();
				}
			}
		} catch (Exception e) {
			LOG.error("OTP validation failed with an internal error", e);
			// the exception text is not shown to the user
			context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, context.form()
					.createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
		}
	}

	private boolean checkRetryAfter(AuthenticationFlowContext context) {

		AuthenticatorConfigModel config = context.getAuthenticatorConfig();
		AuthenticationSessionModel authSession = context.getAuthenticationSession();

		LocalDateTime resendOtpBloackTime = LocalDateTime.parse(authSession.getAuthNote("resendOtpBloackTime"));
		long retryTimeConfig = Integer.parseInt(config.getConfig().get("retryAfterTime"));

		LocalDateTime retryAfter = resendOtpBloackTime.plusMinutes(retryTimeConfig);
		LocalDateTime currentTime = LocalDateTime.now();

		LOG.info(String.format("Try resend otp after <%s>", String.valueOf(retryAfter)));

		if (currentTime.isAfter(retryAfter)) {
			authSession.setAuthNote("resendOtpCount", String.valueOf(0));
			// start a new block window next time, otherwise the limit would never apply again after the first block
			authSession.removeAuthNote("resendOtpBloackTime");
			return true;
		} else {
			return false;
		}
	}

	private boolean isResendOtpLimitExceeded(AuthenticationFlowContext context) {

		AuthenticationSessionModel authSession = context.getAuthenticationSession();
		AuthenticatorConfigModel config = context.getAuthenticatorConfig();

		int count = Integer.parseInt(context.getAuthenticationSession().getAuthNote("resendOtpCount"));
		authSession.setAuthNote("resendOtpCount", String.valueOf(count + 1));

		int resendOtpLimit = Integer.parseInt(config.getConfig().get("resendOtpLimit")) + 1;
		int resendOtpCount = Integer.parseInt(context.getAuthenticationSession().getAuthNote("resendOtpCount"));

		if (resendOtpCount >= resendOtpLimit) {
			LocalDateTime currTime = LocalDateTime.now();
			if (authSession.getAuthNote("resendOtpBloackTime") == null)
				authSession.setAuthNote("resendOtpBloackTime", String.valueOf(currTime));
			return true;
		} else {
			return false;
		}
	}

	private void removeSessionOTP(AuthenticationFlowContext context) {
		context.getAuthenticationSession().removeAuthNote("code");
		context.getAuthenticationSession().removeAuthNote("ttl");
	}

	/**
	 * Checks if cookie is present and is for the current user
	 * 
	 * @param context AuthenticationFlowContext
	 * @return boolean. True if Cookie is Present else False
	 */
	private boolean hasCookie(AuthenticationFlowContext context) {
		Cookie cookie = context.getHttpRequest().getHttpHeaders().getCookies().get(Constants.COOKIE_2FA_ANSWERED);
		if (cookie != null) {
			String encryptedToken = getEncryptedCookieString(context);
			String encryptedCookieValue = cookie.getValue();
			if (encryptedCookieValue != null && MessageDigest.isEqual(
					encryptedCookieValue.getBytes(StandardCharsets.UTF_8), encryptedToken.getBytes(StandardCharsets.UTF_8))) {
				LOG.info(Constants.COOKIE_2FA_ANSWERED + " cookie is set and valid.");
				return true;
			}
		}
		return false;
	}

	/**
	 * Sets an Encrypted Cookie for the user.
	 * 
	 * @param context AuthenticationFlowContext
	 */
	private void setCookie(AuthenticationFlowContext context) {

		String rememberMe = context.getAuthenticationSession().getAuthNote("rememberMe");

		if (!rememberMe.equalsIgnoreCase("on")) {
			return;
		}
		int maxCookieAge = 0;
		if (context.getAuthenticatorConfig() != null) {
			maxCookieAge = 60 * 60 * 24
					* Integer.valueOf(context.getAuthenticatorConfig().getConfig().get("cookieMaxAge"));
		}
		URI uri = context.getUriInfo().getBaseUriBuilder().path("realms").path(context.getRealm().getName()).build();
		// the cookie is only sent over https when Keycloak itself is reached over https
		boolean secure = "https".equalsIgnoreCase(context.getUriInfo().getBaseUri().getScheme());
		addCookie(Constants.COOKIE_2FA_ANSWERED, getEncryptedCookieString(context), uri.getRawPath(), null, null,
				maxCookieAge, secure, true);
	}

	/**
	 * Returns an Encrypted String for the User logged in
	 * 
	 * @param context
	 * @return String
	 */
	private String getEncryptedCookieString(AuthenticationFlowContext context) {
		return encryptToken(context, getUserAgentId(context));
	}

	/**
	 * Encrypts String passed
	 * 
	 * @param context AuthenticationFlowContext
	 * @param value   String to be encrypted
	 * @return String. Encrypted String
	 */
	private String encryptToken(AuthenticationFlowContext context, String value) {
		String algorithm = context.getSession().tokens().signatureAlgorithm(TokenCategory.INTERNAL);
		SignatureSignerContext signer = context.getSession().getProvider(SignatureProvider.class, algorithm).signer();
		return new JWSBuilder().jsonContent(value).sign(signer);
	}

	/**
	 * Fetches user agent from HTTP headers and appends it to username.
	 * 
	 * @param context AuthenticationFlowContext
	 * @return String
	 */
	private String getUserAgentId(AuthenticationFlowContext context) {
		MultivaluedMap<String, String> headers = context.getHttpRequest().getHttpHeaders().getRequestHeaders();
		String username = context.getUser().getUsername();
		String userAgent = headers.getFirst("User-Agent");
		String ip = context.getConnection().getRemoteAddr();
		return username + "_" + userAgent + "_" + ip;
	}

	/**
	 * Creates and adds a cookie to HTTP headers
	 * 
	 * @param name     Name of Cookie
	 * @param value    Value of the Cookie
	 * @param path
	 * @param domain
	 * @param comment
	 * @param maxAge   Validity of cookie
	 * @param secure
	 * @param httpOnly
	 */
	private static void addCookie(String name, String value, String path, String domain, String comment, int maxAge,
			boolean secure, boolean httpOnly) {
		HttpResponse response = ResteasyProviderFactory.getInstance().getContextData(HttpResponse.class);
		StringBuilder cookieBuf = new StringBuilder();
		ServerCookie.appendCookieValue(cookieBuf, 1, name, value, path, domain, comment, maxAge, secure, httpOnly,
				null);
		String cookie = cookieBuf.toString();
		response.getOutputHeaders().add(HttpHeaders.SET_COOKIE, cookie);
	}

	/**
	 * Method sends Email with OTP
	 * 
	 * @param session KeycloakSession
	 * @param realm   Realm of the user
	 * @param user    User
	 * @param code    OTP code to be sent
	 */
	private void sendEmailWithCode(KeycloakSession session, RealmModel realm, UserModel user, String code) {
		if (user.getEmail() == null) {
			LOG.warnf("Could not send access code email due to missing email. realm=%s user=%s", realm.getId(),
					user.getUsername());
			throw new AuthenticationFlowException(AuthenticationFlowError.INVALID_USER);
		}

		Map<String, Object> mailBodyAttributes = new HashMap<>();
		mailBodyAttributes.put("username", user.getUsername());
		mailBodyAttributes.put("code", code);

		String realmName = realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName();
		List<Object> subjectParams = List.of(realmName);
		try {
			EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class);
			emailProvider.setRealm(realm);
			emailProvider.setUser(user);
			emailProvider.send("emailCodeSubject", subjectParams, "code-email.ftl", mailBodyAttributes);
		} catch (EmailException eex) {
			LOG.errorf(eex, "Failed to send access code email. realm=%s user=%s", realm.getId(), user.getUsername());
		}
	}

	@Override
	public boolean requiresUser() {
		return true;
	}

	@Override
	public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
		// return user.getFirstAttribute("mobile_number") != null;
		return true;
	}

	@Override
	public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
	}

	@Override
	public void close() {
	}

}
