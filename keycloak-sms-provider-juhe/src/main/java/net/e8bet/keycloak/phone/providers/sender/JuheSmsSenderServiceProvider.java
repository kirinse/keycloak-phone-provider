package net.e8bet.keycloak.phone.providers.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.client.utils.URIBuilder;
import cc.coopersoft.keycloak.phone.providers.constants.MessageSendResult;
import cc.coopersoft.keycloak.phone.providers.constants.TokenCodeType;
import cc.coopersoft.keycloak.phone.providers.exception.MessageSendException;
import cc.coopersoft.keycloak.phone.providers.spi.MessageSenderService;
import cc.coopersoft.keycloak.phone.utils.PhoneNumber;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.RealmModel;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JuheSmsSenderServiceProvider implements MessageSenderService {
    private static final Logger logger = Logger.getLogger(JuheSmsSenderServiceProvider.class);
    private final Config.Scope config;
    private final RealmModel realm;
    private final OkHttpClient client;
    private static final String SERVER_URL = "http://v.juhe.cn/sms/send";

    public JuheSmsSenderServiceProvider(Config.Scope config, RealmModel realm) {
        this.config = config;
        this.realm = realm;
        this.client = new OkHttpClient().newBuilder()
                .build();
    }

    @Override
    public MessageSendResult sendSmsMessage(TokenCodeType type, PhoneNumber phoneNumber, String code, int expires) throws MessageSendException {
        HashMap<String, String> params = new HashMap<>();
        String kindName = type.name().toLowerCase();
        String templateId = Optional.ofNullable(config.get(realm.getName().toLowerCase() + "-" + kindName + "-template"))
                .orElse(config.get(kindName + "-template"));
        String apiKey = Optional.ofNullable(config.get(realm.getName().toLowerCase() + "-key"))
                .orElse(config.get("key"));
        String tplValue = String.format("#code#=%s", code);

        params.put("tpl_id", templateId);
        params.put("mobile", phoneNumber.getPhoneNumber());
        params.put("tpl_value", tplValue);
        params.put("key", apiKey);

        Request request = new Request.Builder()
                .url(appendParameterToUrl(params))
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                ObjectMapper objectMapper = new ObjectMapper();
                JuheResult resp = objectMapper.readValue(response.body().string(), JuheResult.class);
                logger.info(resp + ": sms sent successfully");
                if (resp.getErrorCode() != 0) {
                    throw new MessageSendException(resp.getErrorCode(),
                            String.valueOf(resp.getErrorCode()),
                            resp.getReason());
                }
                return new MessageSendResult(1).setResendExpires(60).setExpires(expires);
            } else {
                throw new MessageSendException(response.code(),
                        String.valueOf(response.code()),
                        response.message());
            }
        } catch (IOException e) {
            logger.errorv(e,
                    "Failed to send SMS to {0} with contents: {1}. An IOException occurred while communicating with SMS service {0}.",
                    phoneNumber, tplValue);
            throw new MessageSendException();
        }
    }

    @Override
    public void close() {

    }

    private String appendParameterToUrl(Map<String, String> params) {
        try {
            URIBuilder uriBuilder = new URIBuilder(JuheSmsSenderServiceProvider.SERVER_URL);

            if (params != null) {
                for (Map.Entry<String, String> p : params.entrySet()) {
                    uriBuilder.setParameter(p.getKey(), p.getValue());
                }
            }
            return uriBuilder.build().toString();
        } catch (URISyntaxException ignored) {
            return null;
        }
    }
}
