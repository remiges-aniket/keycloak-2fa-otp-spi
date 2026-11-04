package co.remiges.tech.cookieservice;

import org.jboss.logging.Logger;
import org.keycloak.cookie.CookieProvider;
import org.keycloak.cookie.CookieType;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

/**
 * Once a login has completed (captcha, OTP, session limit and required actions all passed), the browser no longer
 * needs the cookies that only tracked the login attempt. They are expired in the login response.
 * The SSO cookie (KEYCLOAK_IDENTITY) is kept, because the web app restores its session from it on every page load.
 */
public class LoginCookieCleanupListener implements EventListenerProvider {

    private static final Logger LOG = Logger.getLogger(LoginCookieCleanupListener.class);
    private static final CookieType[] LOGIN_ATTEMPT_COOKIES = {
            CookieType.AUTH_SESSION_ID, CookieType.AUTH_RESTART, CookieType.LOCALE };

    private final KeycloakSession session;

    public LoginCookieCleanupListener(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() != EventType.LOGIN) {
            return;
        }
        try {
            CookieProvider cookies = session.getProvider(CookieProvider.class);
            for (CookieType type : LOGIN_ATTEMPT_COOKIES) {
                cookies.expire(type);
            }
        } catch (RuntimeException e) {
            // the login itself must not fail because of cookie clean-up
            LOG.warn("Could not expire the login-attempt cookies: " + e.getClass().getSimpleName());
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // admin events are not used
    }

    @Override
    public void close() {
        // nothing to release
    }
}
