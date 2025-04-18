package org.javaprojects.onlinestore.services;

import org.javaprojects.onlinestore.entities.Item;
import org.javaprojects.onlinestore.repositories.ItemsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
class CatalogServiceTest {

    @Autowired
    private ItemsRepository itemsRepository;

    @Autowired
    private CatalogService catalogService;

    @BeforeEach
    void setUp()
    {
        Item item1 = new Item(1L, "Smartphone", "Latest model smartphone with advanced features.", BigDecimal.valueOf(699.99), "/images/smartphone.jpg", 0);
        Item item2 = new Item(2L, "Laptop", "High-performance laptop suitable for gaming and work.", BigDecimal.valueOf(1199.50), "images/laptop.jpg'", 0);
        Item item3 = new Item(3L, "Headphones", "Noise-cancelling over-ear headphones with superior sound quality.", BigDecimal.valueOf(199.95), "/images/headphones.jpg", 0);
        Item item4 = new Item(4L, "Smartwatch", "Feature-packed smartwatch with fitness tracking capabilities.", BigDecimal.valueOf(249.99), "/images/smartwatch.jpg", 0);
        Item item5 = new Item(5L, "Camera", "Digital camera with high resolution and optical zoom.", BigDecimal.valueOf(449.00), "/images/camera.jpg", 0);
        itemsRepository.save(item1).block();
        itemsRepository.save(item2).block();
        itemsRepository.save(item3).block();
        itemsRepository.save(item4).block();
        itemsRepository.save(item5).block();
    }

    @Test
    void getItemsInBasket() {
        Flux<Item> itemPage = itemsRepository.findBySearchString("laptop", PageRequest.of(0, 10));
        Item item = itemPage.blockFirst();
        assert item != null;
        assertEquals("Laptop", item.getTitle());
        assertEquals(1199.50, item.getPrice().doubleValue());
    }

    @Test
    void buyItemsInBasket() {
        itemsRepository.findBySearchString("Smartphone", PageRequest.of(0, 10))
            .flatMap(item -> catalogService.incrementQuantity(item.getId())).then().block();
        itemsRepository.findBySearchString("laptop", PageRequest.of(0, 10))
            .flatMap(item ->
                catalogService.incrementQuantity(item.getId())
                    .then(catalogService.incrementQuantity(item.getId()))
            )
            .then().block();
        Long id = catalogService.buyItemsInBasket().block();
        assertEquals(3098.99, Objects.requireNonNull(catalogService.getOrderById(id).block()).getTotalSum().doubleValue());
    }
}
