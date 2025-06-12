package com.example.demo;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
public class CurrentUserController {

    @GetMapping
    public String currentUser(@AuthenticationPrincipal Jwt principal) {
        return "Logged in as:" + principal.getSubject();
    }
}