package org.javaprojects.onlinestore.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Utility class for security-related operations, providing methods to handle user authentication and roles.
 * It includes a predefined anonymous user with a specific role and username.
 */
public class SecurityUtil
{
    public static final AuthUser ANONYMOUS_USER = createAnonymousUser();

    private SecurityUtil() {
        // Utility class
    }
    /**
     * Role for anonymous users.
     */
    public static final String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";
    private static final String ANONYMOUS_USER_NAME = "anonymousUser";

    /**
     * Creates an anonymous user with a predefined username and role.
     *
     * @return an AuthUser instance representing an anonymous user
     */
    private static AuthUser createAnonymousUser() {
        return new AuthUser(
            -1L,
            ANONYMOUS_USER_NAME,
            "",
            true,
            List.of(ROLE_ANONYMOUS)
        );
    }

    /**
     * Returns the current authenticated user or an anonymous user if no authentication is present.
     *
     * @return a Mono containing the current AuthUser
     */
    public static Mono<AuthUser> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getPrincipal)
            .cast(AuthUser.class);
    }
}
