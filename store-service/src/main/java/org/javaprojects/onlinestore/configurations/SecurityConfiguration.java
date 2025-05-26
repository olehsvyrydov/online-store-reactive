package org.javaprojects.onlinestore.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;

import java.net.URI;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfiguration
{
    /**
     * This method configures the security filter chain for the application.
     * It sets up form login, logout, security context repository, authorization rules,
     * and exception handling.
     *
     * @param http the ServerHttpSecurity object to configure
     * @return the SecurityWebFilterChain object
     */
    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http)
    {
        return http
            .csrf(csrf -> csrf
                .requireCsrfProtectionMatcher(new PathPatternParserServerWebExchangeMatcher("/logout", HttpMethod.POST))
                .disable()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .authenticationSuccessHandler(authenticationSuccessHandler())
            )

            .logout(l -> l
                .logoutSuccessHandler(logoutSuccessHandler())
            )

            .securityContextRepository(
                new WebSessionServerSecurityContextRepository()
            )

            .authorizeExchange(ex -> ex
                .pathMatchers("/login", "/register", "/auth/register").permitAll()
                .pathMatchers(HttpMethod.GET, "/", "/main/items/**", "/items/**", "/images/**").permitAll()
                .anyExchange().authenticated()
            )

            .exceptionHandling(handling -> handling
                .accessDeniedHandler((exchange, denied) ->
                    exchange.getPrincipal().map(principal ->
                        new AccessDeniedException("Access Denied. Principal %s".formatted(principal))
                    ).then())
                .authenticationEntryPoint(
                    new RedirectServerAuthenticationEntryPoint("/login"))
            )

            .build();
    }

    @Bean
    public ServerAuthenticationSuccessHandler authenticationSuccessHandler()
    {
        return new RedirectServerAuthenticationSuccessHandler("/main/items");
    }

    @Bean
    public ServerLogoutSuccessHandler logoutSuccessHandler()
    {
        RedirectServerLogoutSuccessHandler handler = new RedirectServerLogoutSuccessHandler();
        handler.setLogoutSuccessUrl(URI.create("/login?logout"));
        return handler;
    }

    @Bean
    PasswordEncoder passwordEncoder()
    {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
