package cc.coopersoft.keycloak.phone.providers.rest;

import cc.coopersoft.keycloak.phone.providers.constants.TokenCodeType;
import cc.coopersoft.keycloak.phone.providers.spi.PhoneMessageService;
import com.google.i18n.phonenumbers.NumberParseException;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.KeycloakSession;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.HashMap;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

public class TokenCodeResource {

    private static final Logger logger = Logger.getLogger(TokenCodeResource.class);
    protected final KeycloakSession session;
    protected final TokenCodeType tokenCodeType;

    TokenCodeResource(KeycloakSession session, TokenCodeType tokenCodeType) {
        this.session = session;
        this.tokenCodeType = tokenCodeType;
    }

    @GET
    @NoCache
    @Path("")
    @Produces(APPLICATION_JSON)
    public Response getTokenCode(@QueryParam("phoneNumber") String phoneNumber) {
        if (phoneNumber == null) throw new BadRequestException("Must inform a phone number");

        logger.infov("Requested {0} code to {1}", tokenCodeType.getLabel(), phoneNumber);
        HashMap<String, String> body = new HashMap<>();
        try {
            int tokenExpiresIn = session.getProvider(PhoneMessageService.class).sendTokenCode(phoneNumber, tokenCodeType);
            body.put("expires_in", Integer.toString(tokenExpiresIn));
            return Response.ok(body, APPLICATION_JSON_TYPE).build();
        } catch (ClientErrorException e) {
            body.put("error", "abusedMessageService");
            return Response.status(Response.Status.TOO_MANY_REQUESTS).entity(body).build();
        } catch (NumberParseException npe) {
            body.put("error", "invalidPhoneNumber");
            return Response.status(Response.Status.BAD_REQUEST).entity(body).build();
        } catch (ServiceUnavailableException e) {
            body.put("error", "serviceUnavailable");
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(body).build();
        } catch (Exception e) {
            body.put("error", "internalServerError");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(body).build();
        }
    }
}
