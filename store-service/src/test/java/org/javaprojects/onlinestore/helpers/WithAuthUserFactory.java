package org.javaprojects.onlinestore.helpers;

import org.javaprojects.onlinestore.security.AuthUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class WithAuthUserFactory implements WithSecurityContextFactory<WithAuthUser>
{
    @Override
    public SecurityContext createSecurityContext(WithAuthUser a) {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        AuthUser principal = new AuthUser(
            a.id(),
            a.username(),
            "{noop}password",
            true,
            List.of(a.roles()));
        ctx.setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, "password", principal.getAuthorities()));
        return ctx;
    }
}
