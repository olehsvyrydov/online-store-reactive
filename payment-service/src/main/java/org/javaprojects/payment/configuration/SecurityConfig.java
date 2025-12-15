package org.javaprojects.payment.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig
{
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http)
    {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)

            .authorizeExchange(ex -> ex
                .pathMatchers("/actuator/**").permitAll()
                .anyExchange().authenticated()
            )

            .oauth2ResourceServer(customizer -> customizer
                .jwt(jwtSpec -> {
                    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
                    converter.setJwtGrantedAuthoritiesConverter(roleConverter);
                })
            );

        return http.build();
    }

    @SuppressWarnings("unchecked")
    private final Converter<Jwt, Collection<GrantedAuthority>> roleConverter = jwt -> {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        Map<String, Object> account = (Map<String, Object>) resourceAccess.get("account");
        List<String> roles = (List<String>) account.get("roles");
        log.debug("Got roles from JWT: {}", roles);
        return roles.stream()
            .map(SimpleGrantedAuthority::new)
            .map(GrantedAuthority.class::cast)
            .toList();
    };

}
