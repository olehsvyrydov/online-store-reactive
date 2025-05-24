package org.javaprojects.onlinestore.controllers;

import org.javaprojects.onlinestore.services.CatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.javaprojects.onlinestore.utils.SecurityUtil.currentUser;

/**
 * This class is used to handle all requests related to the orders of items.
 * It contains methods to get all orders, get order by id and buy items in the basket.
 */
@Controller
public class OrdersController {
    private final CatalogService catalogService;

    public OrdersController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * This method is used to get all orders from the database and display them on the orders page.
     * @param model model
     * @return orders.html
     */
    @GetMapping("/orders")
    public Mono<String> getOrders(Model model) {
        return catalogService.findAllOrders()
            .collectList()
            .doOnNext(orders ->
                model.addAttribute("orders", orders != null ? orders : List.of())
            ).then(Mono.just("orders"));
    }

    /**
     * This method is used to get order by id from the database and display it on the order page.
     * @param id id of the order
     * @param isNewOrder boolean value to check if the order is new
     * @param model model
     * @return order.html
     */
    @GetMapping("/orders/{id}")
    public Mono<String> getOrderById(
            @PathVariable("id") Long id,
            @RequestParam(value = "newOrder", defaultValue = "false") Boolean isNewOrder,
            Model model
    ) {
        return catalogService.getOrderById(id)
                .doOnNext(orderModel -> model.addAttribute("order", orderModel))
            .then(Mono.just("order"));
    }

    /**
     * Buying items in the basket and clearing it
     * @return redirect to the order page
     */
    @PostMapping("/buy")
    public Mono<String> buy() {
        return currentUser().flatMap(catalogService::buyItemsInBasket)
            .map(id -> "redirect:/orders/" + id + "?newOrder=true");
    }
}
