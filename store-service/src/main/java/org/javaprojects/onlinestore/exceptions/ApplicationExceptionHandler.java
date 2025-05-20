package org.javaprojects.onlinestore.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@ControllerAdvice
public class ApplicationExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApplicationExceptionHandler.class);

    @ExceptionHandler(WrongQuantityException.class)
    public Mono<String> handleIllegalStateException(WrongQuantityException e, Model model, ServerWebExchange exchange) {
        return exchange.getSession()
            .doOnNext(sess ->
                sess.getAttributes().put("error",
                    "Wrong quantity. The quantity must be greater than 0")
            )
            .thenReturn("redirect:/items/" + e.getItemId());
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    public Mono<String> handleIllegalArgumentException(Exception e, Model model) {
        return Mono.just("404");
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public Mono<String> handleInsufficientFundsException(InsufficientFundsException ex, ServerWebExchange exchange) {
        return exchange.getSession()
            .doOnNext(sess ->
                sess.getAttributes().put("error",
                    "Low balance. Your balance is " + ex.getBalance()))
            .thenReturn("redirect:/cart/items");
    }

    @ExceptionHandler({IllegalStateException.class})
    public Mono<String> handleIllegalStateException(Exception e, Model model) {
        log.error("Unexpected error", e);
        return Mono.just("/redirect:/items");
    }
}
