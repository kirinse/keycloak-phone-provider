package io.jumbo.keycloak.phone.providers.sender;

import cc.coopersoft.keycloak.phone.providers.constants.TokenCodeType;
import cc.coopersoft.keycloak.phone.providers.exception.MessageSendException;
import cc.coopersoft.keycloak.phone.providers.spi.MessageSenderService;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.RealmModel;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class JuheSmsSenderServiceProvider implements MessageSenderService {
    private final Config.Scope config;
    private final RealmModel realm;
    private static final String KEY_PARAM_NAME = "juhe_key";

    private Supplier<CloseableHttpClient> httpClient = JuheSmsSenderServiceProvider::newCloseableHttpClient;

    //发送验证码的请求路径URL
    private static final String SERVER_URL = "http://v.juhe.cn/sms/send";

    private static final Logger logger = Logger.getLogger(JuheSmsSenderServiceProvider.class);

    public JuheSmsSenderServiceProvider(Config.Scope config, RealmModel realm) {
        this.config = config;
        this.realm = realm;
    }

    @Override
    public void close() {
    }

    @Override
    public void sendSmsMessage(TokenCodeType type, String phoneNumber, String code, int expires) throws MessageSendException {
        HashMap<String, String> params = new HashMap<>();
        String apiKey = Optional.ofNullable(config.get(realm.getName().toUpperCase() + "_" + KEY_PARAM_NAME))
                .orElse(config.get(KEY_PARAM_NAME));


        String tplValue = String.format("#code#=%s", code);
        String templateId = config.get(realm.getName().toUpperCase() + "_" + type.name().toUpperCase() + "_TEMPLATE");

        params.put("tplid", templateId);
        params.put("mobile", phoneNumber);
        params.put("tpl_value", tplValue);
        params.put("key", apiKey);

        try (CloseableHttpClient client = httpClient.get()) {
            JuheResult result = SimpleHttp.doGet(appendParameterToUrl(params), client).asJson(JuheResult.class);
            logger.infov("----- juhe send result: {0}", result);
            if (result.getErrorCode() != 0) {
                logger.errorv("---- juhe send error: %d, coz: %s.", result.getErrorCode(), result.getReason());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve keys", e);
        }
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

    public static CloseableHttpClient newCloseableHttpClient() {
        return HttpClientBuilder.create().build();
    }
}
