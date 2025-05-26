package org.javaprojects.onlinestore.security;

import org.javaprojects.onlinestore.entities.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Represents an authenticated user in the application.
 * Implements UserDetails to provide user information for Spring Security.
 */
public class AuthUser implements UserDetails
{
    private final Long id;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthUser(AppUser row) {
        this.id          = row.getId();
        this.username    = row.getUsername();
        this.password    = row.getPassword();
        this.enabled     = Boolean.TRUE.equals(row.getEnabled());
        this.authorities = row.getRoles().stream()
            .map(SimpleGrantedAuthority::new)
            .toList();
    }

    public AuthUser(long id, String username, String s, boolean b, List<String> roles)
    {
        this.id          = id;
        this.username    = username;
        this.password    = s; // password is already encoded
        this.enabled     = b;
        this.authorities = roles.stream()
            .map(SimpleGrantedAuthority::new)
            .toList();
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String  getPassword()        { return password; }
    @Override public String  getUsername()        { return username; }
    @Override public boolean isEnabled()          { return enabled; }

    public Long getId() { return id; }
}
