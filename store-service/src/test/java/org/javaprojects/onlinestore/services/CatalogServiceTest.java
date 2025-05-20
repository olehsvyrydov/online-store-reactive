package org.javaprojects.onlinestore.services;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.javaprojects.onlinestore.entities.Item;
import org.javaprojects.onlinestore.helpers.RedisTestContainer;
import org.javaprojects.onlinestore.repositories.ItemsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
class CatalogServiceTest extends RedisTestContainer
{

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
        Item item1 = new Item(1L, "Smartphone", "Latest model smartphone with advanced features.", BigDecimal.valueOf(699.99), "/images/smartphone.jpg", 0L);
        Item item2 = new Item(2L, "Laptop", "High-performance laptop suitable for gaming and work.", BigDecimal.valueOf(1199.50), "images/laptop.jpg'", 0L);
        Item item3 = new Item(3L, "Headphones", "Noise-cancelling over-ear headphones with superior sound quality.", BigDecimal.valueOf(199.95), "/images/headphones.jpg", 0L);
        Item item4 = new Item(4L, "Smartwatch", "Feature-packed smartwatch with fitness tracking capabilities.", BigDecimal.valueOf(249.99), "/images/smartwatch.jpg", 0L);
        Item item5 = new Item(5L, "Camera", "Digital camera with high resolution and optical zoom.", BigDecimal.valueOf(449.00), "/images/camera.jpg", 0L);
        itemsRepository.save(item1).block();
        itemsRepository.save(item2).block();
        itemsRepository.save(item3).block();
        itemsRepository.save(item4).block();
        itemsRepository.save(item5).block();
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
                .withBody("{\"success\":true,\"error\":null, \"currentBalance\":100.0}")
                .withStatus(200)));
        itemsRepository.findBySearchString("Smartphone", PageRequest.of(0, 10))
            .flatMap(item -> catalogService.incrementQuantity(item.getId())).then().block();
        itemsRepository.findBySearchString("laptop", PageRequest.of(0, 10))
            .flatMap(item ->
                catalogService.incrementQuantity(item.getId())
                    .then(catalogService.incrementQuantity(item.getId()))
            )
            .then().block();
        Long id = catalogService.buyItemsInBasket().block();
        assertEquals(3098.99, Objects.requireNonNull(catalogService.getOrderById(id).block()).getTotalSum().doubleValue());
    }
}
