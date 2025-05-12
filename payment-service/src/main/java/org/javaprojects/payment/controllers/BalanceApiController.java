package org.javaprojects.payment.controllers;

import org.javaprojects.payment.dtos.GetBalanceResponse;
import org.javaprojects.payment.services.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class BalanceApiController implements BalanceApi {

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
        GetBalanceResponse response = new GetBalanceResponse();
        response.setBalance(paymentService.getBalance());
        return Mono.just(ResponseEntity.ok(response));
    }
}
