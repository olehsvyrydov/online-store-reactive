package org.javaprojects.onlinestore.services;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.javaprojects.onlinestore.entities.Item;
import org.javaprojects.onlinestore.helpers.DummyOauth2TestConfiguration;
import org.javaprojects.onlinestore.helpers.RedisTestContainer;
import org.javaprojects.onlinestore.repositories.ItemsRepository;
import org.javaprojects.onlinestore.security.AuthUser;
import org.javaprojects.onlinestore.helpers.WithAuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Import(DummyOauth2TestConfiguration.class)
@WithAuthUser
class CatalogServiceTest extends RedisTestContainer
{
    private static final Logger log = LoggerFactory.getLogger(CatalogServiceTest.class);
    @Autowired
    private ItemsRepository itemsRepository;

    @Autowired
    private CatalogService catalogService;

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
        .options(wireMockConfig()
            .dynamicPort())
        .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry)
    {
        String baseUrl = "http://localhost:" + wireMockServer.getPort();
        registry.add("api.online-store.path", () -> baseUrl);
    }

    @BeforeEach
    void setUp()
    {
        Item item1 = new Item(1L, "Smartphone", "Latest model smartphone with advanced features.", BigDecimal.valueOf(699.99), "/images/smartphone.jpg");
        Item item2 = new Item(2L, "Laptop", "High-performance laptop suitable for gaming and work.", BigDecimal.valueOf(1199.50), "images/laptop.jpg'");
        Item item3 = new Item(3L, "Headphones", "Noise-cancelling over-ear headphones with superior sound quality.", BigDecimal.valueOf(199.95), "/images/headphones.jpg");
        Item item4 = new Item(4L, "Smartwatch", "Feature-packed smartwatch with fitness tracking capabilities.", BigDecimal.valueOf(249.99), "/images/smartwatch.jpg");
        Item item5 = new Item(5L, "Camera", "Digital camera with high resolution and optical zoom.", BigDecimal.valueOf(449.00), "/images/camera.jpg");
        itemsRepository.save(item1)
            .then(itemsRepository.save(item2))
            .then(itemsRepository.save(item3))
            .then(itemsRepository.save(item4))
            .then(itemsRepository.save(item5))
            .block();
    }

    @Test
    void getItemsInBasket() {
        Flux<Item> itemPage = itemsRepository.findBySearchString("laptop", PageRequest.of(0, 10));
        Item item = itemPage.blockFirst();
        assert item != null;
        assertEquals("Laptop", item.getTitle());
        assertEquals(1199.50, item.getPrice().doubleValue());
    }

    @Test
    void buyItemsInBasket() {
        wireMockServer.stubFor(get(urlPathEqualTo("/balance"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"balance\":4000.0}")
                .withStatus(200)));
        wireMockServer.stubFor(get(urlPathMatching("/pay/.*"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"success\":true,\"error\":null, \"currentBalance\":1.01}")
                .withStatus(200)));

        // run
        getCurrentUser()
            .flatMap(
                authUser ->
                    itemsRepository
                        .findBySearchString("Smartphone", PageRequest.of(0, 10))

                        .flatMap(item ->
                            catalogService.incrementQuantity(item.getId(), authUser)
                        )
                        .concatWith(
                            itemsRepository.findBySearchString("laptop", PageRequest.of(0, 10))
                                .flatMap(item ->
                                    catalogService.incrementQuantity(item.getId(), authUser)
                                        .then(catalogService.incrementQuantity(item.getId(), authUser))
                                ))
                        .then(
                            catalogService.getItemsInBasket()
                                .doOnNext(itemModel -> log.info("Item in basket: {}", itemModel)).then()
                        )
                        .then(
                            catalogService.buyItemsInBasket(authUser)
                        )
                        .flatMap(id ->
                            catalogService.getOrderById(id, authUser)
                        )

                        // check
                        .doOnNext(order -> assertEquals(3098.99, order.getTotalSum().doubleValue()))
            )
            .block();

    }

    private Mono<AuthUser> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(ctx -> Mono.justOrEmpty(ctx.getAuthentication()))
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getPrincipal)
            .cast(AuthUser.class);
    }
}
