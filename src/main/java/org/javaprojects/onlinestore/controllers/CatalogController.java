package org.javaprojects.onlinestore.controllers;

import org.javaprojects.onlinestore.enums.Action;
import org.javaprojects.onlinestore.enums.Sorting;
import org.javaprojects.onlinestore.services.CatalogService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * This class is used to handle all requests related to the catalog of items.
 * It contains methods to get all items, get item by id, update items in basket and get main page.
 */
@Controller
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * This method is used to get the main page of the application.
     * @return redirect to the items page
     */
    @GetMapping("/")
    public Mono<String> getMainPage() {
        return Mono.just("redirect:/main/items");
    }

    /**
     * This method is used to get all items from the database and display them on the main page.
     * @param searchString search string
     * @param sorting sorting type
     * @param pageSize page size
     * @param pageNumber page number
     * @param model model
     * @return main.html
     */
    @Cacheable(value = "items", key = "#searchString + #sorting + #pageSize + #pageNumber")
    @GetMapping("/main/items")
    public Mono<String> getAllProducts(
            @RequestParam(value = "search", defaultValue = "") String searchString,
            @RequestParam(value = "sort", defaultValue = "NO") Sorting sorting,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "pageNumber", defaultValue = "0") int pageNumber,
            Model model) {

        return catalogService.findAllItems(pageNumber, pageSize, searchString, sorting)
                .doOnNext(allItemsModel -> {
                    model.addAttribute("items", allItemsModel.getProductList());
                    model.addAttribute("paging", allItemsModel.getPaging());
                    model.addAttribute("search", searchString);
                    model.addAttribute("sort", sorting);
                })
                .then(Mono.just("main"));
    }

    /**
     * This method is used to update the count of items in the basket.
     * @param id id of the item
     * @param action action to be performed on the item
     * @return redirect to the item page
     */
    @PostMapping(value = "/main/items/{id}")
    public Mono<String> updateItemsCountInBasket(
        @PathVariable("id") Long id,
        @ModelAttribute Action action)
    {
        return catalogService.updateCountInBasket(id, action.action())
            .then(Mono.just("redirect:/main/items"));
    }
}
