package cc.coopersoft.keycloak.phone.providers.rest;

import cc.coopersoft.keycloak.phone.providers.constants.TokenCodeType;
import org.keycloak.models.KeycloakSession;

import javax.ws.rs.Path;

public class SmsResource {

    private final KeycloakSession session;

    public SmsResource(KeycloakSession session) {
        this.session = session;
    }

    @Path("verification-code")
    public VerificationCodeResource getVerificationCodeResource() {
        return new VerificationCodeResource(session);
    }

    /**
     * 用于登录, 所以要先检查是否存在
     * @return
     */
    @Path("authentication-code")
    public TokenCodeResource getAuthenticationCodeResource() {
        return new TokenCodeResource(session, TokenCodeType.OTP);
    }

    @Path("registration-code")
    public TokenCodeResource getRegistrationCodeResource() {
        return new TokenCodeResource(session, TokenCodeType.REGISTRATION);
    }

    /**
     * 找回密码, 先检查号码是否存在
     * @return
     */
    @Path("reset-code")
    public TokenCodeResource getResetCodeResource() {
        return new TokenCodeResource(session, TokenCodeType.RESET);
    }
}
