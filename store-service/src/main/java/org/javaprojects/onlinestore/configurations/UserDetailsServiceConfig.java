/**
 * Security Classification: Confidential Copyright (c) Yunex Limited 2025. This is an unpublished work, with copyright
 * vested in Yunex Limited. All rights reserved. The information contained herein is the property of Yunex Limited and
 * is provided without liability for any errors or omissions. No part of this document may be copied, reproduced, used,
 * or disclosed except as authorized by contract or with prior written permission. The copyright and the restrictions on
 * reproduction, use, and disclosure apply to all media in which this information may be embodied. Where any information
 * is attributed to individual authors, the views expressed do not necessarily reflect the views of Yunex Limited.
 */
package org.javaprojects.onlinestore.configurations;

import org.javaprojects.onlinestore.repositories.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import reactor.core.publisher.Mono;
import org.javaprojects.onlinestore.utils.AppUserDetails;

@Configuration
public class UserDetailsServiceConfig {

    private final UserRepository repo;

    public UserDetailsServiceConfig(UserRepository repo) {
        this.repo = repo;
    }

    @Bean
    public ReactiveUserDetailsService reactiveUserDetailsService() {
        return username ->
            repo.findByUsername(username)
                .switchIfEmpty(Mono.error(
                    new UsernameNotFoundException(username)))
                .map(AppUserDetails::new);
    }
}

