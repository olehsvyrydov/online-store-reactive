package org.javaprojects.onlinestore.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * This class is used to check the health of the payment service.
 * It queries the readiness endpoint of the payment service to determine if it is up and running.
 */
@Component
public class PaymentHealthClient {

    private final WebClient webClient;

    public PaymentHealthClient(WebClient.Builder builder,
        @Value("${api.online-store.path}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Checks if the payment service is up and running by querying the readiness endpoint.
     *
     * @return a Mono that emits true if the service is up, false otherwise.
     */
    public Mono<Boolean> isUp() {
        return webClient.get()
            .uri("/actuator/health")
            .retrieve()
            .toBodilessEntity()
            .map(r -> true)
            .timeout(Duration.ofSeconds(1))
            .onErrorReturn(false);
    }
}
