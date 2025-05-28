package org.javaprojects.onlinestore.repositories;


import org.javaprojects.onlinestore.entities.Cart;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CartRepository extends ReactiveCrudRepository<Cart, Long> {
    @Query("INSERT INTO cart (item_id, user_id, quantity) VALUES (:itemId, :userId, :quantity)")
    Mono<Void> insertToCart(Long itemId, Long userId, Long quantity);

    @Query("DELETE FROM cart WHERE item_id = :itemId AND user_id = :userId")
    Mono<Void> removeFromCart(Long itemId, Long userId);

    @Query("SELECT * FROM cart WHERE item_id = :itemId AND user_id = :userId")
    Mono<Cart> findByItemIdAndUserId(Long itemId, Long userId);

    @Query("SELECT * FROM cart WHERE user_id = :userId")
    Flux<Cart> findByUserId(Long userId);

    Mono<Void> deleteByUserId(Long userId);
}
