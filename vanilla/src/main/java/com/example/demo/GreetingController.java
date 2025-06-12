package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/greeting")
public class GreetingController {

    @GetMapping("/{name}")
    public String greeting(@PathVariable String name) {
        return "Say Hello to %s at %s".formatted(name, LocalDateTime.now());
    }
}
