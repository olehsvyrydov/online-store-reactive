/**
 * Security Classification: Confidential Copyright (c) Yunex Limited 2025. This is an unpublished work, with copyright
 * vested in Yunex Limited. All rights reserved. The information contained herein is the property of Yunex Limited and
 * is provided without liability for any errors or omissions. No part of this document may be copied, reproduced, used,
 * or disclosed except as authorized by contract or with prior written permission. The copyright and the restrictions on
 * reproduction, use, and disclosure apply to all media in which this information may be embodied. Where any information
 * is attributed to individual authors, the views expressed do not necessarily reflect the views of Yunex Limited.
 */
package org.javaprojects.onlinestore.utils;

import org.javaprojects.onlinestore.entities.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class SecurityUtil
{
    private static final Logger log = LoggerFactory.getLogger(SecurityUtil.class);

    private SecurityUtil() {
        // Utility class
    }
    private static final String ANONYMOUS_USER = "anonymousUser";
    public static final String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";
    private static final AnonymousAuthenticationToken ANONYMOUS_AUTHENTICATION =
        new AnonymousAuthenticationToken(
            "anonymous",
            createAnonymousUser(),
            AuthorityUtils.createAuthorityList(ROLE_ANONYMOUS)
        );

    private static AppUser createAnonymousUser() {
        AppUser anonymousUser = new AppUser();
        anonymousUser.setUsername(ANONYMOUS_USER);
        anonymousUser.setId(-1L);
        anonymousUser.setEnabled(true);
        List<String> roles = new ArrayList<>();
        roles.add(ROLE_ANONYMOUS);
        anonymousUser.setRoles(roles);
        return anonymousUser;
    }

//    public static Mono<AppUser> currentUser() {
//        return ReactiveSecurityContextHolder.getContext()
//            .map(SecurityContext::getAuthentication)
//            .defaultIfEmpty(ANONYMOUS_AUTHENTICATION)
//            .map(Authentication::getPrincipal)
//            .cast(AppUser.class)
//            .map(principal -> {
//                log.debug("currentUser Principal: {}", principal);
//                return principal;
//            });
//    }

    public static Mono<AppUser> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)

            // filter out Spring-added anonymous tokens
            .filter(a -> !(a instanceof AnonymousAuthenticationToken))

            // unwrap our AppUser
            .map(a -> ((AppUserDetails)a.getPrincipal()).domain())

            // fall back to our synthetic anonymous user
            .switchIfEmpty(Mono.just(createAnonymousUser()));
    }
}
