package org.javaprojects.onlinestore.controllers;

import org.javaprojects.onlinestore.enums.Action;
import org.javaprojects.onlinestore.security.AuthUser;
import org.javaprojects.onlinestore.services.CatalogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


@Controller
public class ItemsController {
    private final CatalogService catalogService;
    public ItemsController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * This method is used to get all items in the basket and display them on the cart page.
     * @param id id of the item
     * @param model model
     * @return cart.html
     */
    @GetMapping("/items/{id}")
    public Mono<String> getItemById(
            @PathVariable("id") Long id,
            Model model) {
        return catalogService.getItemById(id)
            .doOnNext(itemModel -> model.addAttribute("itemModel", itemModel))
            .then(Mono.fromSupplier(() -> "item"));
    }

    /**
     * This method is used to update the count of items in the basket.
     * @param id id of the item
     * @param action action to be performed on the item
     * @return redirect to the item page
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/items/{id}")
    public Mono<String> updateItemsCountInBasket(
            @PathVariable("id") Long id,
            @ModelAttribute Action action,
            @AuthenticationPrincipal Mono<AuthUser> authUserMono) {
        return authUserMono.flatMap(authUser ->
                catalogService.updateCountInBasket(id, action.action(), authUser))
            .then(Mono.just("redirect:/items/" + id));
    }

}
