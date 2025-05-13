package org.javaprojects.onlinestore.controllers;

import org.javaprojects.onlinestore.entities.Cart;
import org.javaprojects.onlinestore.entities.Item;
import org.javaprojects.onlinestore.repositories.CartRepository;
import org.javaprojects.onlinestore.repositories.ItemsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class CartControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private CartRepository cartRepository;
    @MockitoBean
    private ItemsRepository itemRepository;

    @BeforeEach
    void setUp() {
        Item item1 = new Item(1L, "Test Title1", "Test Description1", new BigDecimal("19.99"), "test-path1.jpg", 1L);
        Cart cart1 = new Cart(1L);
        Cart cart2 = new Cart(2L);
        Flux<Cart> cartFlux = Flux.fromIterable(List.of(cart1, cart2));

        when(cartRepository.removeFromCart(anyLong())).thenReturn(Mono.empty());
        when(cartRepository.resetItemCount(anyLong())).thenReturn(Mono.empty());
        when(cartRepository.decrementItemCount(anyLong())).thenReturn(Mono.just(item1));
        when(cartRepository.incrementItemCount(anyLong())).thenReturn(Mono.just(item1));
        when(cartRepository.findByItemId(anyLong())).thenReturn(Mono.just(cart1));
        when(itemRepository.findById(anyLong())).thenReturn(Mono.just(item1));
        when(cartRepository.insertToCart(anyLong())).thenReturn(Mono.just(cart1));
        when(cartRepository.findAll()).thenReturn(cartFlux);
    }

    @Test
    void getItemsInBasket() {

        webTestClient.get()
            .uri("/cart/items")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class).consumeWith(response -> {
                   assertNotNull(response.getResponseBody());
            });
    }

    @Test
    void updateItemCountInBasket_plusAction() {
        webTestClient.post()
            .uri(uriBuilder ->
                uriBuilder.path("/cart/items/1")
                    .queryParam("action", "plus")
                    .build())
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader().location("/cart/items");
    }

    @Test
    void updateItemCountInBasket_minusAction() {

        webTestClient.post()
                .uri(uriBuilder ->
                    uriBuilder.path("/cart/items/1")
                        .queryParam("action", "minus")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/cart/items");
    }

    @Test
    void updateItemCountInBasket_deleteAction() {


        webTestClient.post()
                .uri(uriBuilder ->
                    uriBuilder.path("/cart/items/1")
                        .queryParam("action", "delete")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/cart/items");
        verify(cartRepository).removeFromCart(1L);
    }
}
