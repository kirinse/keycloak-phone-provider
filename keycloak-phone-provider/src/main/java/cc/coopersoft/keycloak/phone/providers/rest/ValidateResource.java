package cc.coopersoft.keycloak.phone.providers.rest;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class ValidateResource {

    private static final Logger logger = Logger.getLogger(ValidateResource.class);
    protected final KeycloakSession session;

    ValidateResource(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Validate: user.attributes.phoneNumber, email, username(?)
     * existence
     * @param attribute
     * @param value
     * @return
     */
    @GET
    @NoCache
    @Path("")
    @Produces(APPLICATION_JSON)
    public Response validate(@QueryParam("attribute") String attribute, @QueryParam("value") String value) {
        if (attribute == null || value == null) throw new BadRequestException("missing parameter");
        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();

        logger.debugv("Validate {0} with value {1}", attribute, value);
        try {
            boolean exists = false;
            switch (attribute) {
                case "username":
                    exists = session.users().getUserByUsername(realm, value) != null;
                    break;
                case "user.attributes.phoneNumber":
                    exists = session.users().searchForUserByUserAttributeStream(realm, "phoneNumber", value).findAny().isPresent();
                    break;
                case "email":
                    exists = session.users().getUserByEmail(realm, value) != null;
                    break;
            }
            if (exists) {
                return Response.status(Response.Status.CONFLICT).type(APPLICATION_JSON).build();
            }
            return Response.ok().type(APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(APPLICATION_JSON).build();
        }
    }
}
