package projects.caregiver_backend.controllerTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import projects.caregiver_backend.dtos.request.LoginRequest;
import projects.caregiver_backend.dtos.request.RegisterRequest;
import projects.caregiver_backend.model.Role;
import projects.caregiver_backend.model.User;
import projects.caregiver_backend.repositories.UserRepository;
import projects.caregiver_backend.security.JwtService;
import projects.caregiver_backend.service.AuthService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private User existingUser;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setUsername("testuser");
        validRegisterRequest.setEmail("test@example.com");
        validRegisterRequest.setPassword("password123");

        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsername("testuser");
        validLoginRequest.setPassword("password123");

        existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setUsername("testuser");
        existingUser.setEmail("test@example.com");
        existingUser.setPassword("$2a$10$hashedPassword");
        existingUser.setRole(Role.USER);
    }

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should successfully register new user with valid data")
        void shouldRegisterNewUser() {
            // Given
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
            when(userRepository.save(any(User.class))).thenReturn(existingUser);

            // When
            User result = authService.register(validRegisterRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getRole()).isEqualTo(Role.USER);
            
            verify(userRepository).existsByEmail("test@example.com");
            verify(userRepository).existsByUsername("testuser");
            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> authService.register(validRegisterRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Email already exists")
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(userRepository).existsByEmail("test@example.com");
            verify(userRepository, never()).existsByUsername(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when username already exists")
        void shouldThrowExceptionWhenUsernameExists() {
            // Given
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> authService.register(validRegisterRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Username already exists")
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(userRepository).existsByEmail("test@example.com");
            verify(userRepository).existsByUsername("testuser");
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email is null")
        void shouldThrowExceptionWhenEmailIsNull() {
            // Given
            validRegisterRequest.setEmail(null);

            // When & Then
            assertThatThrownBy(() -> authService.register(validRegisterRequest))
                    .isInstanceOf(NullPointerException.class);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when username is null")
        void shouldThrowExceptionWhenUsernameIsNull() {
            // Given
            validRegisterRequest.setUsername(null);

            // When & Then
            assertThatThrownBy(() -> authService.register(validRegisterRequest))
                    .isInstanceOf(NullPointerException.class);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when password is null")
        void shouldThrowExceptionWhenPasswordIsNull() {
            // Given
            validRegisterRequest.setPassword(null);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> authService.register(validRegisterRequest))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should hash password before saving")
        void shouldHashPasswordBeforeSaving() {
            // Given
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedPassword");
            when(userRepository.save(any(User.class))).thenReturn(existingUser);

            // When
            authService.register(validRegisterRequest);

            // Then
            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(argThat(user -> 
                user.getPassword().equals("$2a$10$hashedPassword")
            ));
        }

        @Test
        @DisplayName("Should set default role to USER")
        void shouldSetDefaultRoleToUser() {
            // Given
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
            when(userRepository.save(any(User.class))).thenReturn(existingUser);

            // When
            authService.register(validRegisterRequest);

            // Then
            verify(userRepository).save(argThat(user -> 
                user.getRole() == Role.USER
            ));
        }

        @Test
        @DisplayName("Should handle special characters in email")
        void shouldHandleSpecialCharactersInEmail() {
            // Given
            validRegisterRequest.setEmail("test+tag@example.co.uk");
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
            when(userRepository.save(any(User.class))).thenReturn(existingUser);

            // When
            User result = authService.register(validRegisterRequest);

            // Then
            assertThat(result).isNotNull();
            verify(userRepository).existsByEmail("test+tag@example.co.uk");
        }

        @Test
        @DisplayName("Should handle case sensitivity in username check")
        void shouldHandleCaseSensitivityInUsername() {
            // Given
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> authService.register(validRegisterRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Username already exists");
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should successfully login with valid credentials")
        void shouldLoginWithValidCredentials() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("password123", existingUser.getPassword())).thenReturn(true);
            when(jwtService.generateToken("testuser", "USER")).thenReturn("mock.jwt.token");

            // When
            String token = authService.login(validLoginRequest);

            // Then
            assertThat(token).isEqualTo("mock.jwt.token");
            verify(userRepository).findByUsername("testuser");
            verify(passwordEncoder).matches("password123", existingUser.getPassword());
            verify(jwtService).generateToken("testuser", "USER");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            LoginRequest request = new LoginRequest();
            request.setUsername("nonexistent");
            request.setPassword("password123");

            // When & Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid credentials")
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);

            verify(userRepository).findByUsername("nonexistent");
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtService, never()).generateToken(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw exception when password is incorrect")
        void shouldThrowExceptionWhenPasswordIncorrect() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("wrongpassword", existingUser.getPassword())).thenReturn(false);

            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("wrongpassword");

            // When & Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid credentials")
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);

            verify(userRepository).findByUsername("testuser");
            verify(passwordEncoder).matches("wrongpassword", existingUser.getPassword());
            verify(jwtService, never()).generateToken(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw exception when username is null")
        void shouldThrowExceptionWhenUsernameIsNullOnLogin() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername(null);
            request.setPassword("password123");

            // When & Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw exception when password is null")
        void shouldThrowExceptionWhenPasswordIsNullOnLogin() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword(null);

            // When & Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should generate JWT with correct role")
        void shouldGenerateJwtWithCorrectRole() {
            // Given
            existingUser.setRole(Role.ADMIN);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtService.generateToken("testuser", "ADMIN")).thenReturn("admin.jwt.token");

            // When
            String token = authService.login(validLoginRequest);

            // Then
            assertThat(token).isEqualTo("admin.jwt.token");
            verify(jwtService).generateToken("testuser", "ADMIN");
        }

        @Test
        @DisplayName("Should handle empty password")
        void shouldHandleEmptyPassword() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("", existingUser.getPassword())).thenReturn(false);

            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("");

            // When & Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("Should handle whitespace in password")
        void shouldHandleWhitespaceInPassword() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("   ", existingUser.getPassword())).thenReturn(false);

            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("   ");

            // When & Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("Should not expose user information in error message")
        void shouldNotExposeUserInformationInError() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.login(validLoginRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid credentials")
                    .hasMessageNotContaining("testuser")
                    .hasMessageNotContaining("not found");
        }
    }
}
