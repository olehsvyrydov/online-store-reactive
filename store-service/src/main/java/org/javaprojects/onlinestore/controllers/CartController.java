package org.javaprojects.onlinestore.controllers;

import org.javaprojects.onlinestore.enums.Action;
import org.javaprojects.onlinestore.services.CatalogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * This class is used to handle all requests related to the cart of items.
 * It contains methods to get all items in the basket and update items in the basket.
 */
@Controller
@RequestMapping("/cart")
public class CartController {
    private final CatalogService catalogService;

    public CartController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public Mono<String> getCartPage() {
        return Mono.just("redirect:/cart/items");
    }

    /**
     * This method is used to get all items in the basket and display them on the cart page.
     * @param model model
     * @return cart.html
     */
    @GetMapping("/items")
    public Mono<String> getItemsInBasket(Model model) {
       return catalogService.getItemsInBasket()
              .collectList()
           .doOnNext(items -> {
               double totalPrice =  items.stream()
                   .mapToDouble(itemModel -> itemModel.getPrice().doubleValue() * itemModel.getCount())
                   .sum();

               model.addAttribute("items", items);
               model.addAttribute("total", totalPrice);
               model.addAttribute("empty", items.isEmpty());
           })
           .thenReturn("cart");

    }

    /**
     * This method is used to update the count of items in the basket.
     * @param id id of the item
     * @param action action to be performed on the item
     * @return redirect to the cart page
     */
    @PreAuthorize("!hasRole('ROLE_ANONIMOUS')")
    @PostMapping("/items/{id}")
    public Mono<String> updateItemCountInBasket(
            @PathVariable("id") Long id,
            @ModelAttribute Action action) {
        return catalogService.updateCountInBasket(id, action.action())
            .thenReturn("redirect:/cart/items");
    }
}
