package org.javaprojects.onlinestore.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@Controller
public class ErrorControllerHandler implements ErrorController {
    @RequestMapping("/error")
    public Mono<String> handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");

        if(statusCode != null) {
            if (statusCode == 404)
                return Mono.just("404");
            if (statusCode == 500)
                return Mono.just("500");
        }
        return Mono.just("main");
    }
}
