package cc.coopersoft.keycloak.phone.providers.rest;

import org.keycloak.models.KeycloakSession;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

public class ValidationResource {
    private final KeycloakSession session;

    public ValidationResource(KeycloakSession session) {
        this.session = session;
    }

    @Path("")
    @GET
    public VerificationCodeResource getValidationResource() {
        return new VerificationCodeResource(session);
    }
}
