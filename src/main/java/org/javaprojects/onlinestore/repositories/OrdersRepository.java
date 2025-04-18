package org.javaprojects.onlinestore.repositories;

import org.javaprojects.onlinestore.entities.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface OrdersRepository extends ReactiveCrudRepository<Order, Long>
{
    Flux<Order> findBy(Long orderId);
}
