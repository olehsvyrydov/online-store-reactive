package org.javaprojects.onlinestore.controllers;

import org.javaprojects.onlinestore.enums.Action;
import org.javaprojects.onlinestore.enums.Sorting;
import org.javaprojects.onlinestore.models.AllItemsModel;
import org.javaprojects.onlinestore.models.ItemModel;
import org.javaprojects.onlinestore.models.Paging;
import org.javaprojects.onlinestore.services.CatalogService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

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
    @GetMapping("/main/items")
    public Mono<String> getAllProducts(
            @RequestParam(value = "search", defaultValue = "") String searchString,
            @RequestParam(value = "sort", defaultValue = "NO") Sorting sorting,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "pageNumber", defaultValue = "0") int pageNumber,
            Model model) {

        return catalogService.findAllItems(pageNumber, pageSize, searchString, sorting)
            .zipWith(catalogService.getItemsCount())
            .map(tuple -> {
                List<ItemModel> items = tuple.getT1();
                // cache does not keep the long value, so we need to cast to Number at first then take a long value.
                long total = ((Number) tuple.getT2()).longValue();
                PageRequest pageable = PageRequest.of(pageNumber, pageSize);
                PageImpl<ItemModel> page = new PageImpl<>(items, pageable, total);

                return new AllItemsModel(items, new Paging(
                    pageNumber,
                    pageSize,
                    page.hasNext(),
                    page.hasPrevious()
                ));
            })
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
