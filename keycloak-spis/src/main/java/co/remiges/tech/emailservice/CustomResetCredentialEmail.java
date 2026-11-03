package co.remiges.tech.emailservice;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.actiontoken.resetcred.ResetCredentialsActionToken;
import org.keycloak.authentication.authenticators.resetcred.ResetCredentialEmail;
import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.DefaultActionTokenKey;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;

import co.remiges.tech.Constants;
import co.remiges.tech.emailservice.EmailService.EmailResponse;
import co.remiges.tech.util.BrandingUtil;
import co.remiges.tech.util.ConfigUtil;
import co.remiges.tech.util.TemplateUtil;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;


public class CustomResetCredentialEmail extends ResetCredentialEmail {

    private static final Logger logger = Logger.getLogger(CustomResetCredentialEmail.class);
    private EmailService emailService = new EmailService();


 @Override
 public void authenticate(AuthenticationFlowContext context) {
     logger.debug("Custom Reset Credential Email");
     AuthenticatorConfigModel config = context.getAuthenticatorConfig();
     Map<String, String> cfg = BrandingUtil.withBranding(config.getConfig(), context.getRealm());
     String forgotPasswordEmailMessage = ConfigUtil.get(cfg, Constants.CFG_MSG_RESET_EMAIL_SENT,
             Constants.DEFAULT_MSG_RESET_EMAIL_SENT);
     UserModel user = context.getUser();
     if (user == null) {
         // an unknown user gets the same answer as a known one, so the screen cannot be used to find valid usernames
         logger.debug("Reset credentials requested for an unknown user");
         context.forkWithSuccessMessage(new FormMessage(forgotPasswordEmailMessage));
         return;
     }
     AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();
     String username = user.getUsername();
     EmailResponse emailResponse;

    String actionTokenUserId = authenticationSession.getAuthNote(DefaultActionTokenKey.ACTION_TOKEN_USER_ID);
    if (actionTokenUserId != null && Objects.equals(user.getId(), actionTokenUserId)) {
            logger.infof("Forget-password triggered when reauthenticating user after authentication via action token. Skipping " + PROVIDER_ID + " screen and using user '%s' ", user.getUsername());
            context.success();
            return;
    }
     int validityInSecs = context.getRealm().getActionTokenGeneratedByUserLifespan(ResetCredentialsActionToken.TOKEN_TYPE);
     int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;

     String authSessionEncodedId = AuthenticationSessionCompoundId.fromAuthSession(authenticationSession).getEncodedId();
     ResetCredentialsActionToken token = new ResetCredentialsActionToken(
    user.getId(),
    user.getEmail(),
    absoluteExpirationInSecs,
    authSessionEncodedId,
    authenticationSession.getClient().getClientId());

     // Create a link to the reset password page
     String link = UriBuilder
         .fromUri(context.getActionTokenUrl(token.serialize(context.getSession(), context.getRealm(), context.getUriInfo())))
         .build()
         .toString();

     // the link holds the reset token, so it is never written to the log
     String emailId = user.getFirstAttribute(
             ConfigUtil.get(cfg, Constants.CFG_USER_EMAIL_ATTRIBUTE, Constants.DEFAULT_USER_EMAIL_ATTRIBUTE));

     Map<String, String> placeholders = new HashMap<>();
     placeholders.put(Constants.PH_BRANDING_NAME, cfg.get(Constants.CFG_BRANDING_NAME));
     placeholders.put(Constants.PH_USERNAME, username);
     placeholders.put(Constants.PH_FIRST_NAME, user.getFirstName());
     placeholders.put(Constants.PH_LAST_NAME, user.getLastName());
     placeholders.put(Constants.PH_LINK, link);
     String subject = TemplateUtil.render(ConfigUtil.get(cfg, Constants.CFG_RESET_EMAIL_SUBJECT_TEMPLATE,
             Constants.DEFAULT_RESET_EMAIL_SUBJECT_TEMPLATE), placeholders);
     // user values and the link are HTML-escaped, the body is HTML
     String htmlMessage = TemplateUtil.renderHtml(ConfigUtil.get(cfg, Constants.CFG_RESET_EMAIL_BODY_TEMPLATE,
             Constants.DEFAULT_RESET_EMAIL_BODY_TEMPLATE), placeholders);
     EmailSender emailSender = EmailSender.fromConfig(cfg, Constants.CFG_RESET_EMAIL_FROM_NAME,
             Constants.CFG_RESET_EMAIL_FROM_ADDRESS, Constants.CFG_RESET_EMAIL_TAGS);
     String apiKey = config.getConfig().get("netcoreApiKey");
     String apiUrl = config.getConfig().get("netcoreApiUrl");
     String proxyHost = config.getConfig().get("proxyHost");
     String proxyPort = config.getConfig().get("proxyPort");
     boolean skiptls = Boolean.parseBoolean(config.getConfig().get("skiptls"));
     boolean isProxyRequired = Boolean.parseBoolean(config.getConfig().get("isProxyRequired"));
     if ((emailId != null) &&  (!isProxyRequired) ){
         try {
             emailResponse = emailService.sendEmailWithoutProxy(emailId, subject, htmlMessage, emailSender, apiKey, apiUrl);
             if (!emailResponse.isSuccess()) {
                context.failure(AuthenticationFlowError.INTERNAL_ERROR, null);
                logger.error("Failed to send email: " + emailResponse.getErrorMessage());
            } else {
                context.forkWithSuccessMessage(new FormMessage(forgotPasswordEmailMessage));
                return;
            }

         } catch (Exception e) {
             logger.error("Failed to send email", e);
             // Create a Response that indicates an error and pass it to context.failure
             Response response = Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                        .entity("Failed to send email due to server error").build();
             context.failure(AuthenticationFlowError.ACCESS_DENIED, response);
         }
     } else if ((emailId != null) &&  (isProxyRequired)){
        if (ConfigUtil.isBlank(proxyHost) || ConfigUtil.isBlank(proxyPort)) {
            logger.error("Proxy host or port is null");
            // Create a Response that indicates an error and pass it to context.failure
            Response response = Response.status(Response.Status.BAD_REQUEST)
                                       .entity("Proxy host or port is null").build();
            context.failure(AuthenticationFlowError.INTERNAL_ERROR, response);
            return;
        }
        try {
           emailResponse = emailService.sendEmailWithProxy(emailId, subject, htmlMessage, emailSender, apiKey, apiUrl,proxyHost,proxyPort,skiptls);
            if (!emailResponse.isSuccess()) {
                context.failure(AuthenticationFlowError.ACCESS_DENIED, null);
                logger.error("Failed to send email: " + emailResponse.getErrorMessage());
            } else {
                context.forkWithSuccessMessage(new FormMessage(forgotPasswordEmailMessage));
                return;
            }
        } catch (Exception e) {
            logger.error("Failed to send email" + e.getMessage(), e);
            // Create a Response that indicates an error and pass it to context.failure
            Response response = Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                       .entity("Failed to send email due to server error").build();
            context.failure(AuthenticationFlowError.INTERNAL_ERROR, response);
        }

     }else{
         // The user sees the same message as for a user with an email, so the screen does not show which users exist
         logger.error("Email ID is null or invalid for user: " + user.getUsername());
         context.forkWithSuccessMessage(new FormMessage(forgotPasswordEmailMessage));
     }
 }
}
