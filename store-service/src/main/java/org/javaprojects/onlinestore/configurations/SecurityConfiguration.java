/**
 * Security Classification: Confidential Copyright (c) Yunex Limited 2025. This is an unpublished work, with copyright
 * vested in Yunex Limited. All rights reserved. The information contained herein is the property of Yunex Limited and
 * is provided without liability for any errors or omissions. No part of this document may be copied, reproduced, used,
 * or disclosed except as authorized by contract or with prior written permission. The copyright and the restrictions on
 * reproduction, use, and disclosure apply to all media in which this information may be embodied. Where any information
 * is attributed to individual authors, the views expressed do not necessarily reflect the views of Yunex Limited.
 */
package org.javaprojects.onlinestore.configurations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import reactor.core.publisher.Mono;
import java.net.URI;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfiguration
{
    private static final Logger log = LoggerFactory.getLogger(SecurityConfiguration.class);
    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .formLogin(form -> form
                .loginPage("/login")
                .authenticationSuccessHandler(authenticationSuccessHandler())
            )
            .logout(l -> l
//                .logoutUrl("/logout")
                .logoutSuccessHandler(logoutSuccessHandler()))
            .securityContextRepository(new WebSessionServerSecurityContextRepository())
            .authorizeExchange(ex -> ex
                .pathMatchers("/login","/register", "/auth/register").permitAll()
                .pathMatchers(HttpMethod.GET, "/","/main/items/**","/items/**", "/images/**").permitAll()
                .anyExchange().authenticated())
            .httpBasic(Customizer.withDefaults())
            .anonymous(ServerHttpSecurity.AnonymousSpec::disable)
            .exceptionHandling(handling -> handling
                .accessDeniedHandler((exchange, denied) ->
                    Mono.error(new AccessDeniedException("Access Denied")))
            )
            .build();
    }

    @Bean
    public ServerAuthenticationSuccessHandler authenticationSuccessHandler() {
        return new RedirectServerAuthenticationSuccessHandler("/main/items");
    }

    @Bean
    public ServerLogoutSuccessHandler logoutSuccessHandler() {
        RedirectServerLogoutSuccessHandler handler = new RedirectServerLogoutSuccessHandler();
        handler.setLogoutSuccessUrl(URI.create("/main/items"));
        return handler;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
