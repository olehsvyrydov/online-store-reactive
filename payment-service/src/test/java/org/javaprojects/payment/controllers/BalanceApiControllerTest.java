package org.javaprojects.payment.controllers;

import org.javaprojects.payment.dtos.GetBalanceResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;


import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "app.initial-balance=100.0"
        }
)
@AutoConfigureMockMvc
class BalanceApiControllerTest
{
    @Autowired
    private TestRestTemplate restTemplate;
    @LocalServerPort
    int localPort;
    @Test
    void getBalance()
    {
        var balance = restTemplate.getForEntity(
            "http://localhost:"+localPort+"/balance", GetBalanceResponse.class);
        assertTrue(balance.getStatusCode().is2xxSuccessful());
        assertNotNull(balance.getBody());
        assertEquals(100.0F, balance.getBody().getBalance());
    }
}
