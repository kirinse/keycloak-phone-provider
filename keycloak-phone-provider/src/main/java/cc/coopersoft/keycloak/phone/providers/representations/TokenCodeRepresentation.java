package cc.coopersoft.keycloak.phone.providers.representations;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.security.SecureRandom;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenCodeRepresentation {

    private String id;
    private String phoneNumber;
    private String code;
    private String type;
    private Date createdAt;
    private Date expiresAt;
    private Boolean confirmed;

    public static TokenCodeRepresentation forPhoneNumber(String phoneNumber, int length) {

        TokenCodeRepresentation tokenCode = new TokenCodeRepresentation();

        tokenCode.id = KeycloakModelUtils.generateId();
        tokenCode.phoneNumber = phoneNumber;
        tokenCode.code = generateTokenCode(length);
        tokenCode.confirmed = false;

        return tokenCode;
    }

    private static String generateTokenCode(int length) {
        if (length < 4) {
            length = 4;
        }
        SecureRandom secureRandom = new SecureRandom();
        float start = (float) Math.pow(10, length - 1);
        float end = (float) (9 * Math.pow(10, length - 1));
        Integer code = (int) (start + secureRandom.nextFloat() * end);
//        Integer code = secureRandom.nextInt(999_999);
        return String.format("%06d", code);
    }
}
