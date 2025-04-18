package org.javaprojects.onlinestore.controllers;

import org.javaprojects.onlinestore.entities.Item;
import org.javaprojects.onlinestore.models.ItemModel;
import org.javaprojects.onlinestore.services.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
class ItemsControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        Item item1 = new Item(1L, "Test Title1", "Test Description1", new BigDecimal("19.99"), "test-path1.jpg", 1);
        ItemModel itemModel1 = new ItemModel(
            item1.getId(),
            item1.getTitle(),
            item1.getDescription(),
            item1.getPrice(),
            item1.getImgPath(),
            item1.getCount()
        );

        when(catalogService.getItemById(anyLong())).thenReturn(Mono.just(itemModel1));
        when(catalogService.updateCountInBasket(anyLong(), any())).thenReturn(Mono.empty());
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
}
