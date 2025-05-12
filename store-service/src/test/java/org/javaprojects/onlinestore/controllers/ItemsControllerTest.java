package org.javaprojects.onlinestore.controllers;

import org.javaprojects.onlinestore.entities.Item;
import org.javaprojects.onlinestore.helpers.RedisTestContainer;
import org.javaprojects.onlinestore.repositories.ItemsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
class ItemsControllerTest extends RedisTestContainer
{
    @Autowired
    private WebTestClient webTestClient;

    @MockitoSpyBean
    private ItemsRepository repo;

    @BeforeEach
    void setUp() {
        Item item1 = new Item(1L, "Test Title1", "Test Description1", new BigDecimal("19.99"), "test-path1.jpg", 1);
        repo.save(item1).block();
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
            });
        // check caching
        webTestClient.get()
            .uri("/items/1")
            .exchange()
            .expectStatus().isOk();
        verify(repo, times(1)).findById(1L);
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

        webTestClient.get().uri(uriBuilder -> uriBuilder
                .path("/main/items")
                .queryParam("search", "")
                .queryParam("sort", "NO")
                .queryParam("pageSize", "10")
                .queryParam("pageNumber", "0")
                .build())
            .exchange()
            .expectStatus().isOk();

        verify(repo, times(1)).findBy(any());
    }
}
