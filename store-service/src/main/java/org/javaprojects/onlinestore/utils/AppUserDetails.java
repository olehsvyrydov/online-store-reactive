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
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.Objects;

public class AppUserDetails extends org.springframework.security.core.userdetails.User {
    private final AppUser appUser;

    public AppUserDetails(AppUser appUser) {
        super(
            appUser.getUsername(),
            appUser.getPassword(),          // hash!
            appUser.getEnabled(),
            true, true, true,
            AuthorityUtils.createAuthorityList(appUser.getRoles().toArray(String[]::new))
        );
        this.appUser = appUser;
    }
    public AppUser domain() { return appUser; }

    @Override
    public boolean equals(Object o)
    {
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        AppUserDetails that = (AppUserDetails) o;
        return Objects.equals(appUser, that.appUser);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), appUser);
    }
}
