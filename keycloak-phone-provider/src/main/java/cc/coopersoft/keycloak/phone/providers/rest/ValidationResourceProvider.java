package cc.coopersoft.keycloak.phone.providers.rest;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class ValidationResourceProvider implements RealmResourceProvider {
    private final KeycloakSession session;

    ValidationResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new ValidateResource(session);
    }

    @Override
    public void close() {

    }
}
