package org.javaprojects.payment.controllers;

import org.javaprojects.payment.dtos.UpdateBalanceResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "app.initial-balance=10.0"
        })
class PaymentApiControllerTest
{
    @Autowired
    private TestRestTemplate restTemplate;
    @LocalServerPort
    int localPort;
    @Test
    void makePayment_Success()
    {
        var response = restTemplate.getForEntity("http://localhost:" + localPort + "/pay/1.0", UpdateBalanceResponse.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertEquals(Boolean.TRUE, response.getBody().getSuccess());
        assertEquals(9.0F, response.getBody().getCurrentBalance());
        assertNull(response.getBody().getError());
    }

    @Test
    void makePayment_LowBalance()
    {
        var response = restTemplate.getForEntity("http://localhost:" + localPort + "/pay/20.0", UpdateBalanceResponse.class);
        assertTrue(response.getStatusCode().is4xxClientError());
        assertNotNull(response.getBody());
        assertEquals(Boolean.FALSE, response.getBody().getSuccess());
        assertEquals(10.0F, response.getBody().getCurrentBalance());
        assertNotNull(response.getBody().getError());
    }

    @Test
    void makePayment_BadRequest()
    {
        var response = restTemplate.getForEntity("http://localhost:" + localPort + "/pay/wrong", UpdateBalanceResponse.class);
        assertTrue(response.getStatusCode().is4xxClientError());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getError());
    }
}
