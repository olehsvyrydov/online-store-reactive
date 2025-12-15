package org.javaprojects.onlinestore.security;

import org.javaprojects.onlinestore.repositories.UserRepository;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * This service implements ReactiveUserDetailsService to provide user details from a database.
 * It retrieves user information based on the username and returns a UserDetails object.
 */
@Service
public class DbReactiveUserDetailsService implements ReactiveUserDetailsService
{
    private final UserRepository repo;

    public DbReactiveUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    /**
     * Finds a user by username in a database and returns a UserDetails object.
     *
     * @param username the username of the user to find
     * @return a Mono containing UserDetails if found, or an error if not found
     */
    @Override
    public Mono<UserDetails> findByUsername(String username) {
            return repo.findByUsername(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException(username)))
                .map(AuthUser::new);
    }
}
