package co.remiges.tech.cookieservice;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import co.remiges.tech.Constants;

public class LoginCookieCleanupListenerFactory implements EventListenerProviderFactory {

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new LoginCookieCleanupListener(session);
    }

    @Override
    public void init(Config.Scope config) {
        // no configuration
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // nothing to do after start
    }

    @Override
    public void close() {
        // nothing to release
    }

    @Override
    public String getId() {
        return Constants.LOGIN_COOKIE_CLEANUP_LISTENER_ID;
    }
}
