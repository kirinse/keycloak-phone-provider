package cc.coopersoft.keycloak.phone.providers.spi.impl;

import cc.coopersoft.keycloak.phone.providers.constants.TokenCodeType;
import cc.coopersoft.keycloak.phone.providers.representations.TokenCodeRepresentation;
import cc.coopersoft.keycloak.phone.providers.spi.MessageSenderService;
import cc.coopersoft.keycloak.phone.providers.spi.PhoneMessageService;
import cc.coopersoft.keycloak.phone.providers.spi.TokenCodeService;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Response;
import java.time.Instant;

public class PhoneMessageServiceImpl implements PhoneMessageService {
    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    private static final Logger logger = Logger.getLogger(PhoneMessageServiceImpl.class);
    private final KeycloakSession session;
    private final String service;
    private final int tokenExpiresIn;
    private final int hourMaximum;
    private final int tokenLen;
    private final String[] enabledCountries;

    PhoneMessageServiceImpl(KeycloakSession session, Scope config) {
        this.session = session;
        this.service = session.listProviderIds(MessageSenderService.class)
                .stream().filter(s -> s.equals(config.get("service")))
                .findFirst().orElse(
                        session.listProviderIds(MessageSenderService.class)
                                .stream().findFirst().orElse("")
                );
        this.tokenExpiresIn = config.getInt("token-expires-in", 60);
        this.hourMaximum = config.getInt("hour-maximum", 3);
        this.tokenLen = config.getInt("token-length", 6);
        this.enabledCountries = config.getArray("enabled-regions");
    }

    @Override
    public void close() {
    }


    private TokenCodeService getTokenCodeService() {
        return session.getProvider(TokenCodeService.class);
    }

    @Override
    public int sendTokenCode(String phoneNumber, TokenCodeType type) throws Exception {
        try {
            Phonenumber.PhoneNumber number = phoneUtil.parseAndKeepRawInput(phoneNumber, null);
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
            if ((!isPossible && !isNumberValid) || numberType != PhoneNumberUtil.PhoneNumberType.MOBILE) {
                throw new NumberParseException(NumberParseException.ErrorType.NOT_A_NUMBER, "phoneNumber invalid");
            }
        } catch (NumberParseException e) {
            logger.errorv("------- parse phoneNumber {0}, got {1}", phoneNumber, e);
            throw e;
        }
        if (getTokenCodeService().isAbusing(phoneNumber, type, hourMaximum)) {
//            throw new NotAllowedException("You requested the maximum number of messages the last hour");
            throw new ClientErrorException("You requested the maximum number of messages the last hour", Response.Status.TOO_MANY_REQUESTS);
        }

        TokenCodeRepresentation ongoing = getTokenCodeService().ongoingProcess(phoneNumber, type);
        if (ongoing != null) {
            logger.infov("No need of sending a new {0} code for {1}", type.getLabel(), phoneNumber);
            return (int) (ongoing.getExpiresAt().getTime() - Instant.now().toEpochMilli()) / 1000;
        }

        TokenCodeRepresentation token = TokenCodeRepresentation.forPhoneNumber(phoneNumber, tokenLen);

        try {
//            session.getProvider(MessageSenderService.class, service).sendSmsMessage(type, phoneNumber, token.getCode(), tokenExpiresIn);
            getTokenCodeService().persistCode(token, type, tokenExpiresIn);
            logger.infov("Sent {0} code to {1} over {2}", type.getLabel(), phoneNumber, service);
//        } catch (MessageSendException e) {
        } catch (Exception e) {
//            logger.errorv("Message sending to {0} failed with {1}: {2}",
//                    phoneNumber, e.getErrorCode(), e.getErrorMessage());
            throw new ServiceUnavailableException("Internal server error");
        }

        return tokenExpiresIn;
    }

    @Override
    public String[] getEnabledRegions() {
        return this.enabledCountries;
    }
}
