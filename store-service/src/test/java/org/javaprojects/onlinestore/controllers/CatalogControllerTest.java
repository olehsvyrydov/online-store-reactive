package org.javaprojects.onlinestore.controllers;

import org.javaprojects.onlinestore.configurations.SecurityConfiguration;
import org.javaprojects.onlinestore.enums.Sorting;
import org.javaprojects.onlinestore.models.ItemModel;
import org.javaprojects.onlinestore.security.AuthUser;
import org.javaprojects.onlinestore.services.CatalogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
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
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@ActiveProfiles("test")
@WebFluxTest(controllers = CatalogController.class)
@ContextConfiguration(classes = {CatalogController.class, SecurityConfiguration.class})
class CatalogControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private CatalogService catalogService;

    @Test
    @WithAnonymousUser
    void getMainPage() {
        webTestClient.get().uri("/")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/main/items");
    }

    @Test
    @WithAnonymousUser
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
    @MethodSource("provideSortingAndPageParameters")
    void updateItemsCountInBasket_PlusItem(String action, String securityRole, String redirectPath) {
        when(catalogService.updateCountInBasket(anyLong(), eq(action), any(AuthUser.class))).thenReturn(Mono.empty());

        WebTestClient client = webTestClient.mutateWith(csrf());

        if (!"ROLE_ANONYMOUS".equals(securityRole)) {          // authenticated runs only
            AuthUser user = new AuthUser(1L, "test", "pwd", true, List.of(securityRole));
            client = client.mutateWith(SecurityMockServerConfigurers
                .mockAuthentication(new UsernamePasswordAuthenticationToken(
                    user, "pwd", user.getAuthorities())));
        }

        client
            .post()
            .uri(uriBuilder -> uriBuilder
            .path("/main/items/1")
            .queryParam("action", action)
            .build())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader().location(redirectPath);
    }

    public static List<Arguments> provideSortingAndPageParameters()
    {
        return List.of(
            Arguments.of("PLUS",   "ROLE_USER",       "/main/items"),
            Arguments.of("MINUS",  "ROLE_USER",       "/main/items"),
            Arguments.of("DELETE", "ROLE_USER",       "/main/items"),
            Arguments.of("PLUS",   "ROLE_ADMIN",      "/main/items"),
            Arguments.of("MINUS",  "ROLE_ADMIN",      "/main/items"),
            Arguments.of("DELETE", "ROLE_ADMIN",      "/main/items"),
            Arguments.of("PLUS",   "ROLE_ANONYMOUS",  "/login"),
            Arguments.of("MINUS",  "ROLE_ANONYMOUS",  "/login"),
            Arguments.of("DELETE", "ROLE_ANONYMOUS",  "/login")
        );
    }
}
