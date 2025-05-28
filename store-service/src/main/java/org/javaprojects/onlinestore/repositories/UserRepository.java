package org.javaprojects.onlinestore.repositories;

import org.javaprojects.onlinestore.entities.AppUser;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserRepository extends ReactiveCrudRepository<AppUser, UUID>
{

    Mono<AppUser> findByUsername(String username);

    Mono<Boolean> existsByUsername(String username);
}
