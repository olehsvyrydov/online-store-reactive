package org.javaprojects.onlinestore.models;

public record RegisterDto(
    String username,
    String password,
    String confirmPassword
)
{
}
