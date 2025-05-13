package org.javaprojects.payment.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record ApplicationProperties(
    float initialBalance
) {}

