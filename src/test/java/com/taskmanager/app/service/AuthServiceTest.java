package com.taskmanager.app.service;

import com.taskmanager.app.dto.AuthRequestDTO;
import com.taskmanager.app.exception.InvalidCredentialsException;
import com.taskmanager.app.exception.UserAlreadyExistsException;
import com.taskmanager.app.model.User;
import com.taskmanager.app.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtService jwtService;

    @InjectMocks
    AuthService authService;

    @Test
    @DisplayName("registers a new user when username is unique")
    void registersNewUser() {
        AuthRequestDTO req = new AuthRequestDTO();
        req.setUsername("alice");
        req.setPassword("password");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("hashed");

        String res = authService.register(req);

        assertThat(res).contains("User registered");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("throws UserAlreadyExistsException when registering an existing user")
    void throwsWhenUserExists() {
        AuthRequestDTO req = new AuthRequestDTO();
        req.setUsername("bob");
        req.setPassword("pass");

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.register(req)).isInstanceOf(UserAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login returns token when credentials valid")
    void loginReturnsToken() {
        AuthRequestDTO req = new AuthRequestDTO();
        req.setUsername("carol");
        req.setPassword("pwd");

        User user = User.builder().username("carol").password("hashed").build();
        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pwd", "hashed")).thenReturn(true);
        when(jwtService.generateToken("carol")).thenReturn("token-abc");

        String token = authService.login(req);

        assertThat(token).isEqualTo("token-abc");
    }

    @Test
    @DisplayName("login throws InvalidCredentialsException on bad password")
    void loginThrowsOnBadPassword() {
        AuthRequestDTO req = new AuthRequestDTO();
        req.setUsername("dave");
        req.setPassword("bad");

        User user = User.builder().username("dave").password("hashed").build();
        when(userRepository.findByUsername("dave")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req)).isInstanceOf(InvalidCredentialsException.class);
    }
}

