/**
 * Security Classification: Confidential Copyright (c) Yunex Limited 2025. This is an unpublished work, with copyright
 * vested in Yunex Limited. All rights reserved. The information contained herein is the property of Yunex Limited and
 * is provided without liability for any errors or omissions. No part of this document may be copied, reproduced, used,
 * or disclosed except as authorized by contract or with prior written permission. The copyright and the restrictions on
 * reproduction, use, and disclosure apply to all media in which this information may be embodied. Where any information
 * is attributed to individual authors, the views expressed do not necessarily reflect the views of Yunex Limited.
 */
package org.javaprojects.onlinestore.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.index.Indexed;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;

@Table("app_user")
public class AppUser{
    @Id
    private Long id;
    @Indexed
    private String username;
    private String password;          // already encoded
    private Boolean enabled;
    @Column("role")
    private List<String> roles;

    public AppUser()
    {
    }

    public AppUser(Long id, String username, String password, Boolean enabled, List<String> roles)
    {
        this.id = id;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.roles = roles;
    }

    public Long getId()
    {
        return id;
    }

    public AppUser setId(Long id)
    {
        this.id = id;
        return this;
    }

    public String getUsername()
    {
        return username;
    }

    public AppUser setUsername(String username)
    {
        this.username = username;
        return this;
    }

    public String getPassword()
    {
        return password;
    }

    public AppUser setPassword(String password)
    {
        this.password = password;
        return this;
    }

    public Boolean getEnabled()
    {
        return enabled;
    }

    public AppUser setEnabled(Boolean enabled)
    {
        this.enabled = enabled;
        return this;
    }

    public List<String> getRoles()
    {
        return roles;
    }

    public AppUser setRoles(List<String> roles)
    {
        this.roles = roles;
        return this;
    }

    @Override
    public String toString()
    {
        return "AppUser{" +
            "id=" + id +
            ", username='" + username + '\'' +
            ", password='" + password + '\'' +
            ", enabled=" + enabled +
            ", roles=" + roles +
            '}';
    }
}
