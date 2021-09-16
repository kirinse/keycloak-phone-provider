package cc.coopersoft.keycloak.phone.authentication.requiredactions;

import cc.coopersoft.keycloak.phone.providers.spi.PhoneMessageService;
import cc.coopersoft.keycloak.phone.providers.spi.TokenCodeService;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.jboss.logging.Logger;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

public class UpdatePhoneNumberRequiredAction implements RequiredActionProvider {
    private static final Logger logger = Logger.getLogger(UpdatePhoneNumberRequiredAction.class);
    public static final String PROVIDER_ID = "UPDATE_PHONE_NUMBER";
    public static final String NOTE_NAME = "UPDATE_PHONE_NUMBER_STEP";
    public static final String STEP_VERIFY = "verify";
    public static final String STEP_ADD = "add";

    public static final String TPL = "login-update-phone-number.ftl";
    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
    }

    private void setEnabledRegionAttribute(RequiredActionContext context, LoginFormsProvider form) {
        Map<String, String> countries = new HashMap<>();
        for (String region : context.getSession().getProvider(PhoneMessageService.class).getEnabledRegions()) {
            countries.put(region, String.format("+%s", phoneUtil.getCountryCodeForRegion(region)));
        }
        form.setAttribute("countries", countries);
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        UserModel user = context.getUser();
        LoginFormsProvider form = context.form();
        AuthenticationSessionModel authSession = context.getSession().getContext().getAuthenticationSession();

        setEnabledRegionAttribute(context, form);
        String currentStep = authSession.getAuthNote(NOTE_NAME);

        if (currentStep == null || currentStep.equals("")) {
            currentStep = STEP_VERIFY;
        }
        logger.infov("===== requiredActionChallenge.currentStep={0}", currentStep);

        if (currentStep.equals(STEP_VERIFY)) {
            String currentPhoneNumber = user.getAttributeStream("phoneNumber").findFirst().orElse(null);
            if (currentPhoneNumber != null) {
                try {
                    Phonenumber.PhoneNumber number = phoneUtil.parse(currentPhoneNumber, null);
                    int countryCode = number.getCountryCode();
                    form.setAttribute("currentCountryCode", String.format("+%s", countryCode));
                    form.setAttribute("currentNumber", String.valueOf(number.getNationalNumber()));
                    form.setAttribute("currentCountry", phoneUtil.getRegionCodeForCountryCode(countryCode));
                } catch (NumberParseException e) {
                    currentStep = STEP_ADD;
                }
            } else {
                currentStep = STEP_ADD;
            }
        }
        authSession.setAuthNote(NOTE_NAME, currentStep);
        Response challenge = form.createForm(TPL);
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        TokenCodeService tokenCodeService = context.getSession().getProvider(TokenCodeService.class);
        LoginFormsProvider form = context.form();
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        String phoneNumber = formData.getFirst("phoneNumber");
        String code = formData.getFirst("code");

        AuthenticationSessionModel authSession = context.getSession().getContext().getAuthenticationSession();
        String currentStep = authSession.getAuthNote(NOTE_NAME);
        logger.infov("===== processAction.currentStep={0}", currentStep);
        try {
            if (currentStep.equals(STEP_VERIFY)) {
                tokenCodeService.validateCodeOnly(context.getUser(), phoneNumber, code);
                authSession.setAuthNote(NOTE_NAME, STEP_ADD);
                setEnabledRegionAttribute(context, form);
                Response challenge = form.createForm(TPL);
                context.challenge(challenge);
            } else if (currentStep.equals(STEP_ADD)) {
                tokenCodeService.validateCode(context.getUser(), phoneNumber, code);
                authSession.removeAuthNote(NOTE_NAME);
                context.success();
            }
        } catch (BadRequestException e) {
            Response challenge = context.form()
                    .setError("noOngoingVerificationProcess")
                    .createForm(TPL);
            context.challenge(challenge);
        } catch (ClientErrorException e) {
            Response challenge = context.form()
                    .setAttribute("phoneNumber", phoneNumber)
                    .setError("verificationCodeDoesNotMatch")
                    .createForm(TPL);
            context.challenge(challenge);
        }
    }

    @Override
    public void close() {
    }
}
