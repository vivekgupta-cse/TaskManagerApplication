package com.taskmanager.app.service;

import com.taskmanager.app.dto.AuthRequestDTO;
import com.taskmanager.app.exception.InvalidCredentialsException;
import com.taskmanager.app.exception.UserAlreadyExistsException;
import com.taskmanager.app.model.User;
import com.taskmanager.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public String register(AuthRequestDTO request) {
        log.debug("register called for username={}", request.getUsername());
        // 1. Check if user exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("register failed: user already exists username={}", request.getUsername());
            throw new UserAlreadyExistsException(request.getUsername());
        }

        // 2. Hash the password before saving!
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .build();

        userRepository.save(user);
        log.info("User registered username={}", request.getUsername());
        return "User registered successfully";
    }

    public String login(AuthRequestDTO request) {
        log.debug("login called for username={}", request.getUsername());
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("login failed: invalid credentials username={}", request.getUsername());
            throw new InvalidCredentialsException();
        }

        try {
            String token = jwtService.generateToken(user.getUsername());
            log.debug("login succeeded username={}", request.getUsername());
            return token;
        } catch (Exception ex) {
            log.error("Failed to generate token for username={}", request.getUsername(), ex);
            // Convert any token-generation errors into a runtime exception that's handled by the controller advice
            throw new RuntimeException("Failed to generate authentication token");
        }
    }
}
