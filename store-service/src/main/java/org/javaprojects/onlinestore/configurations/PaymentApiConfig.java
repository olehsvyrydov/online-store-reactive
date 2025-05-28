package org.javaprojects.onlinestore.configurations;

import org.javaprojects.onlinestore.api.BalanceApi;
import org.javaprojects.onlinestore.api.PaymentApi;
import org.javaprojects.onlinestore.client.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PaymentApiConfig {

    @Bean
    ApiClient apiClient(
        ReactiveOAuth2AuthorizedClientManager authManager,
        WebClient.Builder webClientBuilder,
        @Value("${api.online-store.path:http://payment-service:8082}") String basePath) {

        var oauthFilter = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authManager);
        oauthFilter.setDefaultClientRegistrationId("store-service");

        WebClient webClient = webClientBuilder
            .filter(oauthFilter)
            .baseUrl(basePath)
            .build();

        var apiClient = new ApiClient(webClient);
        apiClient.setBasePath(basePath);

        return apiClient;
    }

    @Bean
    public PaymentApi paymentApi(ApiClient apiClient) {
        return new PaymentApi(apiClient);
    }

    @Bean
    public BalanceApi balanceApi(ApiClient apiClient) {
        return new BalanceApi(apiClient);
    }
}
