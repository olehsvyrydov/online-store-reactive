/**
 * Security Classification: Confidential Copyright (c) Yunex Limited 2025. This is an unpublished work, with copyright
 * vested in Yunex Limited. All rights reserved. The information contained herein is the property of Yunex Limited and
 * is provided without liability for any errors or omissions. No part of this document may be copied, reproduced, used,
 * or disclosed except as authorized by contract or with prior written permission. The copyright and the restrictions on
 * reproduction, use, and disclosure apply to all media in which this information may be embodied. Where any information
 * is attributed to individual authors, the views expressed do not necessarily reflect the views of Yunex Limited.
 */
package org.javaprojects.onlinestore.services;

import org.javaprojects.onlinestore.repositories.UserRepository;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DbReactiveUserDetailsService
    implements ReactiveUserDetailsService
{

    private final UserRepository repo;

    public DbReactiveUserDetailsService(UserRepository repo)
    {
        this.repo = repo;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return repo.findByUsername(username)
            .map(u -> User.withUsername(u.getUsername())
                .password(u.getPassword())
                .authorities(u.getRoles()
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList())
                .disabled(!u.getEnabled())
                .build());
    }
}
