package cc.coopersoft.keycloak.phone.utils;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */

public class UserUtils {

    private static UserModel singleUser(Stream<UserModel> users) {
        if (!users.findAny().isPresent()) {
            return null;
        }
        return users
                .filter(u -> u.getAttributeStream("phoneNumberVerified")
                        .anyMatch("true"::equals))
                .findFirst().orElse(null);
    }

    public static UserModel findUserByPhone(UserProvider userProvider, RealmModel realm, String phoneNumber) {
        Stream<UserModel> users = userProvider.searchForUserByUserAttributeStream(
                realm, "phoneNumber", phoneNumber);
        return singleUser(users);
    }

    public static UserModel findUserByPhone(UserProvider userProvider, RealmModel realm, String phoneNumber, String notIs) {
        Stream<UserModel> users = userProvider.searchForUserByUserAttributeStream(
                realm, "phoneNumber", phoneNumber);
        return singleUser(users.filter(u -> !u.getId().equals(notIs)));
    }

    public static boolean isDuplicatePhoneAllowed() {
        //TODO isDuplicatePhoneAllowed
        return true;
    }
}
