package com.securebank.user.service;

import com.securebank.common.exception.BadRequestException;
import com.securebank.common.exception.DuplicateResourceException;
import com.securebank.common.exception.ResourceNotFoundException;
import com.securebank.common.security.JwtUtil;
import com.securebank.user.dto.*;
import com.securebank.user.entity.Role;
import com.securebank.user.entity.User;
import com.securebank.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = new User("john@bank.com", "$2a$10$hashedPassword", "John Doe", "+1234567890", Role.CUSTOMER);
        testUser.setId(testUserId);
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Register - success")
    void register_ShouldCreateUser_WhenEmailIsNew() {
        RegisterRequest request = new RegisterRequest("john@bank.com", "Str0ng!Pass", "John Doe", "+1234567890");

        when(userRepository.existsByEmail("john@bank.com")).thenReturn(false);
        when(passwordEncoder.encode("Str0ng!Pass")).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals("john@bank.com", response.getEmail());
        assertEquals("John Doe", response.getFullName());
        assertEquals("CUSTOMER", response.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register - duplicate email throws exception")
    void register_ShouldThrowException_WhenEmailExists() {
        RegisterRequest request = new RegisterRequest("john@bank.com", "Str0ng!Pass", "John Doe", "+1234567890");

        when(userRepository.existsByEmail("john@bank.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Login - success")
    void login_ShouldReturnToken_WhenCredentialsValid() {
        LoginRequest request = new LoginRequest("john@bank.com", "Str0ng!Pass");

        when(userRepository.findByEmail("john@bank.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Str0ng!Pass", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(testUserId, "john@bank.com", "CUSTOMER")).thenReturn("jwt.token.here");

        AuthResponse response = userService.login(request);

        assertNotNull(response);
        assertEquals("jwt.token.here", response.getToken());
        assertNotNull(response.getUser());
        assertEquals("john@bank.com", response.getUser().getEmail());
    }

    @Test
    @DisplayName("Login - invalid email throws exception")
    void login_ShouldThrowException_WhenEmailNotFound() {
        LoginRequest request = new LoginRequest("unknown@bank.com", "password");

        when(userRepository.findByEmail("unknown@bank.com")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> userService.login(request));
    }

    @Test
    @DisplayName("Login - wrong password throws exception")
    void login_ShouldThrowException_WhenPasswordWrong() {
        LoginRequest request = new LoginRequest("john@bank.com", "WrongPass");

        when(userRepository.findByEmail("john@bank.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPass", "$2a$10$hashedPassword")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> userService.login(request));
    }

    @Test
    @DisplayName("Get profile - success")
    void getProfile_ShouldReturnUser_WhenExists() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        UserResponse response = userService.getProfile(testUserId);

        assertNotNull(response);
        assertEquals(testUserId, response.getId());
        assertEquals("john@bank.com", response.getEmail());
    }

    @Test
    @DisplayName("Get profile - not found throws exception")
    void getProfile_ShouldThrowException_WhenUserNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getProfile(unknownId));
    }
}
