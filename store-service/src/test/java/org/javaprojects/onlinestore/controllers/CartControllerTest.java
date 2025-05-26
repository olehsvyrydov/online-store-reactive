package org.javaprojects.onlinestore.controllers;

import org.javaprojects.onlinestore.entities.Item;
import org.javaprojects.onlinestore.helpers.RedisTestContainer;
import org.javaprojects.onlinestore.models.ItemModel;
import org.javaprojects.onlinestore.repositories.CartRepository;
import org.javaprojects.onlinestore.security.AuthUser;
import org.javaprojects.onlinestore.services.CatalogService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class CartControllerTest extends RedisTestContainer
{
    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private CatalogService catalogService;
    @MockitoBean
    private CartRepository cartRepository;

    @BeforeEach
    void setUp() {
        Item item1 = new Item(1L, "Test Title1", "Test Description1", new BigDecimal("19.99"), "test-path1.jpg");
        Item item2 = new Item(2L, "Test Title2", "Test Description2", new BigDecimal("29.99"), "test-path2.jpg");
        ItemModel itemModel1 = new ItemModel(item1, 1);
        ItemModel itemModel2 = new ItemModel(item2, 2);

        when(catalogService.updateCountInBasket(anyLong(), anyString(), any(AuthUser.class)))
            .thenReturn(Mono.empty());
        when(catalogService.getItemsInBasket()).thenReturn(Flux.fromIterable(List.of(itemModel1, itemModel2)));
    }

    @Test
    void getItemsInBasket() {
        webTestClient
            .mutateWith(getMockAuthentication())
            .get()
            .uri("/cart/items")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class).consumeWith(response -> {
                   assertNotNull(response.getResponseBody());
            });
    }

    @Test
    void updateItemCountInBasket_plusAction() {
        webTestClient
            .mutateWith(getMockAuthentication())
            .mutateWith(csrf())
            .post()
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
        webTestClient
            .mutateWith(getMockAuthentication())
            .mutateWith(csrf())
            .post()
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
        webTestClient
            .mutateWith(getMockAuthentication())
            .mutateWith(csrf())
            .post()
            .uri(uriBuilder ->
                uriBuilder.path("/cart/items/1")
                    .queryParam("action", "delete")
                    .build())
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader().location("/cart/items");
    }

    private static @NotNull WebTestClientConfigurer getMockAuthentication()
    {
        return SecurityMockServerConfigurers.mockAuthentication(
            new UsernamePasswordAuthenticationToken("test", "password", List.of(new SimpleGrantedAuthority(
                "ROLE_USER"))
            )
        );
    }

    @ParameterizedTest
    @MethodSource("provideSortingAndPageParameters")
    void updateItemCountInBasket(String action, String securityRole, String location) {
        WebTestClient client = webTestClient.mutateWith(csrf());

        if (!"ROLE_ANONYMOUS".equals(securityRole)) {          // authenticated runs only
            AuthUser user = new AuthUser(1L, "test", "pwd", true, List.of(securityRole));
            client = client.mutateWith(SecurityMockServerConfigurers
                .mockAuthentication(new UsernamePasswordAuthenticationToken(
                    user, "pwd", user.getAuthorities())));
        }

        client
            .post()
            .uri(uriBuilder ->
                uriBuilder.path("/cart/items/1")
                    .queryParam("action", action)
                    .build())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader().location(location);
    }

    public static List<Arguments> provideSortingAndPageParameters()
    {
        return List.of(
            Arguments.of("PLUS",   "ROLE_USER",       "/cart/items"),
            Arguments.of("MINUS",  "ROLE_USER",       "/cart/items"),
            Arguments.of("DELETE", "ROLE_USER",       "/cart/items"),
            Arguments.of("PLUS",   "ROLE_ADMIN",      "/cart/items"),
            Arguments.of("MINUS",  "ROLE_ADMIN",      "/cart/items"),
            Arguments.of("DELETE", "ROLE_ADMIN",      "/cart/items"),
            Arguments.of("PLUS",   "ROLE_ANONYMOUS",  "/login"),
            Arguments.of("MINUS",  "ROLE_ANONYMOUS",  "/login"),
            Arguments.of("DELETE", "ROLE_ANONYMOUS",  "/login")
        );
    }

    @ParameterizedTest
@ValueSource(strings = {"PLUS", "MINUS", "DELETE"})
void updateItemCountInBasketIf(String action) {
    webTestClient
        .mutateWith(csrf())
        .post()
        .uri(uriBuilder ->
            uriBuilder.path("/cart/items/1")
                .queryParam("action", action)
                .build())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().is3xxRedirection()
        .expectHeader().location("/login");
}
}
