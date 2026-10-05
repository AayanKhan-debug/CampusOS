package com.campusos.service;

import com.campusos.dto.request.LoginRequest;
import com.campusos.dto.request.RegisterRequest;
import com.campusos.dto.response.AuthResponse;
import com.campusos.entity.User;
import com.campusos.enums.Role;
import com.campusos.exception.DuplicateResourceException;
import com.campusos.repository.UserRepository;
import com.campusos.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthServiceTest — unit tests for authentication business logic.
 *
 * We use Mockito to mock all dependencies (UserRepository, PasswordEncoder, etc.)
 * so that tests run without a database or Spring context.
 *
 * @ExtendWith(MockitoExtension.class) → enables Mockito annotations
 * @Mock → creates a mock (fake) implementation
 * @InjectMocks → creates a real AuthService and injects the mocks into it
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegisterRequest;
    private User savedUser;
    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setName("Alice Student");
        validRegisterRequest.setEmail("alice@campus.edu");
        validRegisterRequest.setPassword("password123");
        validRegisterRequest.setDepartment("Computer Science");
        validRegisterRequest.setYear(2);

        savedUser = User.builder()
                .id(1L)
                .name("Alice Student")
                .email("alice@campus.edu")
                .password("$2a$12$hashedpassword")
                .role(Role.STUDENT)
                .department("Computer Science")
                .year(2)
                .build();

        mockUserDetails = new org.springframework.security.core.userdetails.User(
                "alice@campus.edu",
                "$2a$12$hashedpassword",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
    }

    // ── Registration Tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("Register: success → returns token and user info")
    void register_withValidRequest_shouldReturnAuthResponse() {
        // Arrange
        when(userRepository.existsByEmail("alice@campus.edu")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userDetailsService.loadUserByUsername("alice@campus.edu")).thenReturn(mockUserDetails);
        when(jwtUtil.generateToken(mockUserDetails)).thenReturn("mock.jwt.token");

        // Act
        AuthResponse response = authService.register(validRegisterRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("alice@campus.edu");
        assertThat(response.getUser().getRole()).isEqualTo(Role.STUDENT);

        // Verify password was hashed (encode was called with plain password)
        verify(passwordEncoder).encode("password123");
        // Verify user was saved
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register: duplicate email → throws DuplicateResourceException")
    void register_withExistingEmail_shouldThrowDuplicateResourceException() {
        // Arrange
        when(userRepository.existsByEmail("alice@campus.edu")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("alice@campus.edu");

        // Verify we never tried to save a user
        verify(userRepository, never()).save(any());
        // Verify we never tried to hash the password
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("Register: new user always gets STUDENT role")
    void register_newUser_shouldAlwaysHaveStudentRole() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$12$hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mockUserDetails);
        when(jwtUtil.generateToken(any())).thenReturn("token");

        // Act
        AuthResponse response = authService.register(validRegisterRequest);

        // Assert: role must always be STUDENT, never ADMIN
        assertThat(response.getUser().getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    @DisplayName("Register: password is never stored as plain text")
    void register_passwordIsHashed_neverStoredPlainText() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashedvalue");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userArg = invocation.getArgument(0);
            // Assert: the password passed to save() is NOT the plain text password
            assertThat(userArg.getPassword()).isNotEqualTo("password123");
            assertThat(userArg.getPassword()).isEqualTo("$2a$12$hashedvalue");
            return savedUser;
        });
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mockUserDetails);
        when(jwtUtil.generateToken(any())).thenReturn("token");

        // Act
        authService.register(validRegisterRequest);

        // Verify encode was called
        verify(passwordEncoder).encode("password123");
    }

    // ── Login Tests ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Login: valid credentials → returns JWT token")
    void login_withValidCredentials_shouldReturnToken() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("alice@campus.edu");
        loginRequest.setPassword("password123");

        when(authenticationManager.authenticate(any())).thenReturn(null); // success = no exception
        when(userDetailsService.loadUserByUsername("alice@campus.edu")).thenReturn(mockUserDetails);
        when(jwtUtil.generateToken(mockUserDetails)).thenReturn("mock.jwt.token");
        when(userRepository.findByEmail("alice@campus.edu")).thenReturn(Optional.of(savedUser));

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getUser().getEmail()).isEqualTo("alice@campus.edu");
    }

    @Test
    @DisplayName("Login: wrong password → throws BadCredentialsException")
    void login_withWrongPassword_shouldThrowBadCredentialsException() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("alice@campus.edu");
        loginRequest.setPassword("wrongpassword");

        // AuthenticationManager throws BadCredentialsException for wrong password
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }
}
