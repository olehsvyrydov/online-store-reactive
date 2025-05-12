package org.javaprojects.payment.controllers;

import jakarta.validation.ConstraintViolationException;
import org.javaprojects.payment.dtos.UpdateBalanceResponse;
import org.javaprojects.payment.exceptions.LowBalanceException;
import org.javaprojects.payment.services.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class PaymentApiController implements PaymentApi {
    private final PaymentService paymentService;

    public PaymentApiController(PaymentService paymentService)
    {
        this.paymentService = paymentService;
    }

    @Override
    public Mono<ResponseEntity<UpdateBalanceResponse>> makePayment(
        @PathVariable Float amount,
        final ServerWebExchange exchange
    ) {
        float newBalance = paymentService.processPayment(amount);
        return Mono.just(ResponseEntity.ok(new UpdateBalanceResponse(true, newBalance, null)));
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public Mono<ResponseEntity<UpdateBalanceResponse>> handleConstraintViolation(Exception ex) {
        String errorMessage = "Invalid input: " + ex.getMessage();
        return Mono.just(ResponseEntity.badRequest().body(new UpdateBalanceResponse(false, null, errorMessage)));
    }

    @ExceptionHandler(LowBalanceException.class)
    public Mono<ResponseEntity<UpdateBalanceResponse>> handleIllegalArgument(LowBalanceException ex) {
        String errorMessage = "Insufficient balance: " + ex.getMessage();
        return Mono.just(ResponseEntity.badRequest().body(new UpdateBalanceResponse(false, ex.getBalance(), errorMessage)));
    }
}
