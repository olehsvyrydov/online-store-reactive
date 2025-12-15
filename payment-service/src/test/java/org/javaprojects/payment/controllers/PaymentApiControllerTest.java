package org.javaprojects.payment.controllers;

import org.javaprojects.payment.dtos.UpdateBalanceResponse;
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
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "app.initial-balance=10.0"
        })
@AutoConfigureMockMvc
class PaymentApiControllerTest
{
    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    ReactiveJwtDecoder jwtDecoder;

    @Test
    @WithMockUser
    void makePayment_Success()
    {
        webTestClient
            .get()
            .uri("/pay/1.0")
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk()
            .expectBody(UpdateBalanceResponse.class)
            .value(response -> {
                assertNotNull(response);
                assertEquals(Boolean.TRUE, response.getSuccess());
                assertEquals(9.0F, response.getCurrentBalance());
                assertNull(response.getError());
            });
    }

    @Test
    @WithMockUser
    void makePayment_LowBalance()
    {
        webTestClient
            .get()
            .uri("/pay/20.0")
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().is4xxClientError()
            .expectBody(UpdateBalanceResponse.class)
            .value(response -> {
                assertNotNull(response);
                assertEquals(Boolean.FALSE, response.getSuccess());
                assertEquals(10.0F, response.getCurrentBalance());
                assertNotNull(response.getError());
            });
    }

    @Test
    @WithMockUser
    void makePayment_BadRequest()
    {
        webTestClient
            .get()
            .uri("/pay/wrong")
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().is4xxClientError()
            .expectBody(UpdateBalanceResponse.class)
            .value(response -> {
                assertNotNull(response);
                assertNotNull(response.getError());
            });
    }
}
