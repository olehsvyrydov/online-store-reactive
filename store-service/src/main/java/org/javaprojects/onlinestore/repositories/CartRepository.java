package org.javaprojects.onlinestore.repositories;


import org.javaprojects.onlinestore.entities.Cart;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CartRepository extends ReactiveCrudRepository<Cart, Long> {
    @Query("INSERT INTO cart (item_id) VALUES (:itemId)")
    Mono<Void> insertToCart(Long itemId);

    @Query("UPDATE items SET count = count + 1 WHERE id = :itemId")
    Mono<Void> incrementItemCount(Long itemId);

    @Query("DELETE FROM cart WHERE item_id = :itemId")
    Mono<Void> removeFromCart(Long itemId);

    @Query("""
        UPDATE items
        SET count = CASE WHEN count > 0 THEN count - 1 ELSE 0 END
        WHERE id = :itemId
        """)
    Mono<Void> decrementItemCount(Long itemId);

    @Query("UPDATE items SET count = 0 WHERE id = :itemId")
    Mono<Void> resetItemCount(Long itemId);

    @Query("SELECT * FROM cart WHERE item_id = :itemId")
    Mono<Cart> findByItemId(Long itemId);
}
