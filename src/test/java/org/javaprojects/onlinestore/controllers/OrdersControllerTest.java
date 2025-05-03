package org.javaprojects.onlinestore.controllers;

import org.javaprojects.onlinestore.entities.Item;
import org.javaprojects.onlinestore.models.ItemModel;
import org.javaprojects.onlinestore.models.OrderModel;
import org.javaprojects.onlinestore.services.CatalogService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;

@ActiveProfiles("test")
@WebFluxTest(controllers = OrdersController.class)
@ContextConfiguration(classes = {OrdersController.class})
class OrdersControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private CatalogService catalogService;

    @Test
    void getOrders() {
        Item item1 = new Item(1L, "Test Title1", "Test Description1", new BigDecimal("19.99"), "test-path1.jpg", 1);
        Item item2 = new Item(2L, "Test Title2", "Test Description2", new BigDecimal("29.99"), "test-path2.jpg", 2);
        OrderModel orderModel = new OrderModel(1L, List.of(new ItemModel(item1, item1.getCount()), new ItemModel(item2, item1.getCount())), new BigDecimal("79.97"));
        OrderModel orderModel2 = new OrderModel(1L, List.of(new ItemModel(item1, item1.getCount()), new ItemModel(item2, item2.getCount())), new BigDecimal("79.97"));
        Flux<OrderModel> orderModelList = Flux.fromIterable(List.of(orderModel, orderModel2));
        Mockito.when(catalogService.findAllOrders()).thenReturn(orderModelList);
        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    assertNotNull(response.getResponseBody());
                });
    }

    @Test
    void getOrderById() {
        Item item1 = new Item(1L, "Test Title1", "Test Description1", new BigDecimal("19.99"), "test-path1.jpg", 1);
        Item item2 = new Item(2L, "Test Title2", "Test Description2", new BigDecimal("29.99"), "test-path2.jpg", 2);
        OrderModel orderModel = new OrderModel(1L, List.of(new ItemModel(item1, item1.getCount()), new ItemModel(item2, item2.getCount())), new BigDecimal("79.97"));
        Mockito.when(catalogService.getOrderById(anyLong())).thenReturn(Mono.just(orderModel));
        webTestClient.get()
                .uri("/orders/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    assertNotNull(response.getResponseBody());
                });
    }

    @Test
    void buy() {
        Mockito.when(catalogService.buyItemsInBasket()).thenReturn(Mono.just(1L));
        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/orders/1?newOrder=true");
    }
}
