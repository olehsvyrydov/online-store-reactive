package org.javaprojects.onlinestore.repositories;

import org.javaprojects.onlinestore.entities.Item;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ItemsRepository extends ReactiveCrudRepository<Item, Long>
{
    @NonNull
    Flux<Item> findBy(Pageable pageable);

    @Query("""
            SELECT * FROM items AS i
            WHERE LOWER(i.title) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(i.description) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Flux<Item> findBySearchString(@Param("search") String search, Pageable pageable);

    @NonNull
    Mono<Long> count();
}
