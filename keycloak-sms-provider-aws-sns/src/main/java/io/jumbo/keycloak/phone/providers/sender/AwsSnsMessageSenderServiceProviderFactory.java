package io.jumbo.keycloak.phone.providers.sender;

import cc.coopersoft.keycloak.phone.providers.spi.MessageSenderService;
import cc.coopersoft.keycloak.phone.providers.spi.MessageSenderServiceProviderFactory;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class AwsSnsMessageSenderServiceProviderFactory implements MessageSenderServiceProviderFactory {
    private Config.Scope config;
    @Override
    public MessageSenderService create(KeycloakSession session) {
        return new AwsSnsSmsSenderService(session.getContext().getRealm().getDisplayName(), config);
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "aws-sns";
    }
}
