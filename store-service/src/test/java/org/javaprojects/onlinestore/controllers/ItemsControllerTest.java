package org.javaprojects.onlinestore.controllers;

import org.javaprojects.onlinestore.entities.Item;
import org.javaprojects.onlinestore.helpers.RedisTestContainer;
import org.javaprojects.onlinestore.repositories.ItemsRepository;
import org.javaprojects.onlinestore.services.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@WithMockUser
class ItemsControllerTest extends RedisTestContainer
{
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ItemsRepository itemsRepository;

    @Autowired
    private CatalogService catalogService;

    @Autowired
    ReactiveRedisConnectionFactory redisConnectionFactory;
    Item entity = new Item(1L, "Test Title1", "Test Description1",
        BigDecimal.valueOf(19.99), "test-path1.jpg", 1L);
    @BeforeEach
    void setUp()
    {
        // start with an empty cache so first request must hit the DB
        redisConnectionFactory.getReactiveConnection().serverCommands().flushAll().block();
        itemsRepository.save(entity).block();
    }

    @Test
    void getItemById() {
        webTestClient.get()
            .uri("/items/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .consumeWith(response -> {
                assertNotNull(response.getResponseBody());
                System.out.println(redisConnectionFactory.getReactiveConnection().keyCommands()
                    .keys(ByteBuffer.wrap("*".getBytes())).block());
            });
    }

    @Test
    void updateItemCountInBasket_plusAction() {
        webTestClient.post()
            .uri(uriBuilder ->
                uriBuilder.path("/items/1")
                    .queryParam("action", "plus")
                    .build())
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader().location("/items/1");
    }

    @Test
    void updateItemCountInBasket_minusAction() {
        webTestClient.post()
            .uri(uriBuilder ->
                uriBuilder.path("/items/1")
                    .queryParam("action", "minus")
                    .build())
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader().location("/items/1");
    }

    @Test
    void updateItemCountInBasket_deleteAction() {
        webTestClient.post()
            .uri(uriBuilder ->
                uriBuilder.path("/items/1")
                    .queryParam("action", "delete")
                    .build())
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader().location("/items/1");
    }

    @Test
    void getAllItems_checkCache() {
        webTestClient.get().uri(uriBuilder -> uriBuilder
                .path("/main/items")
                .queryParam("search", "")
                .queryParam("sort", "NO")
                .queryParam("pageSize", "10")
                .queryParam("pageNumber", "0")
                .build())
            .exchange()
            .expectStatus().isOk();
    }
}
