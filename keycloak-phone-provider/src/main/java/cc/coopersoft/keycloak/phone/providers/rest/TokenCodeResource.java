package cc.coopersoft.keycloak.phone.providers.rest;

import cc.coopersoft.keycloak.phone.providers.constants.TokenCodeType;
import cc.coopersoft.keycloak.phone.providers.spi.PhoneMessageService;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.KeycloakSession;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

public class TokenCodeResource {

    private static final Logger logger = Logger.getLogger(TokenCodeResource.class);
    protected final KeycloakSession session;
    protected final TokenCodeType tokenCodeType;
    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

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
        try {
            Phonenumber.PhoneNumber number = phoneUtil.parseAndKeepRawInput(phoneNumber, "CN");
            logger.info("-----------------");
            logger.infov("country_code: {0}", number.getCountryCode());
            logger.infov("national_number: {0}", number.getNationalNumber());
            logger.infov("extension: {0}", number.getExtension());
            logger.infov("country_code_source: {0}", number.getCountryCodeSource());
            logger.infov("italian_leading_zero: {0}", number.isItalianLeadingZero());
            logger.infov("raw_input: {0}", number.getRawInput());
            boolean isPossible = phoneUtil.isPossibleNumber(number);
            boolean isNumberValid = phoneUtil.isValidNumber(number);
            PhoneNumberUtil.PhoneNumberType numberType = phoneUtil.getNumberType(number);
            logger.infov("isPossible: {0}", isPossible);
            logger.infov("isNumberValid: {0}", isNumberValid);
            logger.infov("numberType: {0}", numberType);
            logger.info("-----------------");
        } catch (NumberParseException e) {
            logger.errorv("------- parse phoneNumber {0}, got {1}", phoneNumber, e);
            String response = String.format("{\"error\":%s}", "invalid phoneNumber");
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }

        logger.infov("Requested {0} code to {1}", tokenCodeType.getLabel(), phoneNumber);
        int tokenExpiresIn = session.getProvider(PhoneMessageService.class).sendTokenCode(phoneNumber, tokenCodeType);

        String response = String.format("{\"expires_in\":%s}", tokenExpiresIn);

        return Response.ok(response, APPLICATION_JSON_TYPE).build();
    }
}
