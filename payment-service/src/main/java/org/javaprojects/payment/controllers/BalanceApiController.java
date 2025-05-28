package org.javaprojects.payment.controllers;

import org.javaprojects.payment.dtos.GetBalanceResponse;
import org.javaprojects.payment.services.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class BalanceApiController implements BalanceApi {

    private static final Logger log = LoggerFactory.getLogger(BalanceApiController.class);
    private final PaymentService paymentService;

    public BalanceApiController(PaymentService paymentService)
    {
        this.paymentService = paymentService;
    }

    @Override
    @GetMapping(
        value = "/balance",
        produces = { "application/json" }
    )
    public Mono<ResponseEntity<GetBalanceResponse>> getBalance(final ServerWebExchange exchange) {
        log.debug("Received request to get balance");
        GetBalanceResponse response = new GetBalanceResponse();
        response.setBalance(paymentService.getBalance());
        log.debug("Returning balance: {}", response.getBalance());
        return Mono.just(ResponseEntity.ok(response));
    }
}
