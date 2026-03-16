package com.taskmanager.app.controller;

import com.taskmanager.app.dto.AuthRequestDTO;
import com.taskmanager.app.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User Registration and Login")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody AuthRequestDTO request) {
        log.debug("register called for username={}", request.getUsername());
        String res = authService.register(request);
        log.info("User registered username={}", request.getUsername());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody AuthRequestDTO request) {
        log.debug("login attempt for username={}", request.getUsername());
        String token = authService.login(request);
        log.info("User logged in username={}", request.getUsername());
        return ResponseEntity.ok(token);
    }
}
