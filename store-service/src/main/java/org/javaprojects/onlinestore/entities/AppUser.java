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
    @Column("roles")
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
