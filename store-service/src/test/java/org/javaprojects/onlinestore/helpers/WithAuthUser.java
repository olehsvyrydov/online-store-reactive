package org.javaprojects.onlinestore.helpers;

import org.springframework.security.test.context.support.WithSecurityContext;

@WithSecurityContext(factory = WithAuthUserFactory.class)
public @interface WithAuthUser {
    long id() default 1L;
    String username() default "test";
    String[] roles() default {"ROLE_USER"};
}
