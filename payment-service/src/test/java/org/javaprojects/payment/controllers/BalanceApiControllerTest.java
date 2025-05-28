package org.javaprojects.payment.controllers;

import org.javaprojects.payment.dtos.GetBalanceResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(
        properties = {
            "app.initial-balance=100.0"
        }
)
@AutoConfigureMockMvc
class BalanceApiControllerTest
{
    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    ReactiveJwtDecoder jwtDecoder;

    @Test
    @WithMockUser
    void getBalance()
    {
        webTestClient
            .get()
            .uri("/balance")
            .header("Content-Type", "application/json")

            .exchange()
            .expectStatus().isOk()
            .expectBody(GetBalanceResponse.class)
            .value(response ->
            {
                assertNotNull(response);
                assertEquals(100.0F, response.getBalance());
            });
    }
}
