/**
 * Security Classification: Confidential Copyright (c) Yunex Limited 2025. This is an unpublished work, with copyright
 * vested in Yunex Limited. All rights reserved. The information contained herein is the property of Yunex Limited and
 * is provided without liability for any errors or omissions. No part of this document may be copied, reproduced, used,
 * or disclosed except as authorized by contract or with prior written permission. The copyright and the restrictions on
 * reproduction, use, and disclosure apply to all media in which this information may be embodied. Where any information
 * is attributed to individual authors, the views expressed do not necessarily reflect the views of Yunex Limited.
 */
package org.javaprojects.onlinestore.controllers;

import org.javaprojects.onlinestore.entities.AppUser;
import org.javaprojects.onlinestore.models.RegisterDto;
import org.javaprojects.onlinestore.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
public class AuthController
{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder)
    {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public Mono<String> registerPage() {
        return Mono.just("register");
    }

    @GetMapping("/login")
    public Mono<String> loginPage() {
        return Mono.just("login");
    }

    @PostMapping(value = "/auth/register")
    public Mono<String> handleRegister(
        @ModelAttribute RegisterDto dto, Model model) {

        if (!dto.password().equals(dto.confirmPassword())) {
            model.addAttribute("error", "password");
            return Mono.just("redirect:/register");
        }

        return userRepository.existsByUsername(dto.username())
            .flatMap(exists -> {
                if (Boolean.TRUE.equals(exists)) {
                    model.addAttribute("error", "userexists");
                    return Mono.just("redirect:/register");
                }

                var newUser = new AppUser();
                newUser.setUsername(dto.username());
                newUser.setPassword(passwordEncoder.encode(dto.password()));
                newUser.setEnabled(true);
                newUser.setRoles(List.of("ROLE_USER"));

                model.addAttribute("error", "success");
                return userRepository.save(newUser)
                    .thenReturn("redirect:/login?success");
            });
    }


}
