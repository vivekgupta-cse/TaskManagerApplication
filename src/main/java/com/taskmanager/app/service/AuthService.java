package com.taskmanager.app.service;

import com.taskmanager.app.dto.AuthRequestDTO;
import com.taskmanager.app.model.User;
import com.taskmanager.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public String register(AuthRequestDTO request) {
        // 1. Check if user exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        // 2. Hash the password before saving!
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .build();

        userRepository.save(user);
        return "User registered successfully";
    }

    public String login(AuthRequestDTO request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        try {
            return jwtService.generateToken(user.getUsername());
        } catch (Exception ex) {
            // Convert any token-generation errors into a runtime exception that's handled by the controller advice
            throw new RuntimeException("Failed to generate authentication token");
        }
    }
}
