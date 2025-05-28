package org.javaprojects.onlinestore.helpers;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

@TestConfiguration
public class DummyOauth2TestConfiguration
{
    @Bean
    ReactiveClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration reg = ClientRegistration
            .withRegistrationId("dummy")
            .clientId("dummy")
            .clientSecret("dummy")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri("http://localhost/dummy/token")
            .build();

        return new InMemoryReactiveClientRegistrationRepository(reg);
    }
}
