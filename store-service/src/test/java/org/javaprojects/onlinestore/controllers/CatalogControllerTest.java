package org.javaprojects.onlinestore.controllers;

import org.javaprojects.onlinestore.entities.AppUser;
import org.javaprojects.onlinestore.enums.Sorting;
import org.javaprojects.onlinestore.models.ItemModel;
import org.javaprojects.onlinestore.services.CatalogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@WebFluxTest(controllers = CatalogController.class)
@ContextConfiguration(classes = {CatalogController.class})
@WithMockUser
class CatalogControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private CatalogService catalogService;

    @Test
    void getMainPage() {
        webTestClient.get().uri("/")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/main/items");
    }

    @Test
    void getAllProductsWithDefaultParameters() {
        ItemModel item = new ItemModel(1L, "Test Title", "Test Description", new BigDecimal("19.99"), "test-path.jpg", 0);
        List<ItemModel> itemList = Collections.singletonList(item);
        when(catalogService.findAllItems(anyInt(), anyInt(), anyString(), any(Sorting.class))).thenReturn(Flux.fromIterable(itemList));
        when(catalogService.getItemsCount()).thenReturn(Mono.just(10L));
        webTestClient.get().uri(uriBuilder -> uriBuilder
                .path("/main/items")
                .queryParam("search", "")
                .queryParam("sort", "NO")
                .queryParam("pageSize", "10")
                .queryParam("pageNumber", "1")
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .consumeWith(response -> {
                assertNotNull(response.getResponseBody());
            });
    }

    @ParameterizedTest
    @ValueSource(strings = {"PLUS","MINUS","DELETE"})
    void updateItemsCountInBasket_PlusItem(String action) {
        when(catalogService.updateCountInBasket(anyLong(), eq(action), any(Mono.class))).thenReturn(Mono.empty());
        webTestClient.post().uri(uriBuilder -> uriBuilder
                .path("/main/items/1")
                .queryParam("action", action)
                .build())
                .exchange()
                .expectStatus().is3xxRedirection()
            .expectHeader().location("/main/items");
    }
}
