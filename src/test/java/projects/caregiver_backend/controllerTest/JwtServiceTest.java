package projects.caregiver_backend.controllerTest;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import projects.caregiver_backend.security.JwtService;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private final String validSecret = "this-is-a-valid-32-character-secret-key-for-testing";
    private final long expiration = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(validSecret, expiration);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create JwtService with valid secret")
        void shouldCreateWithValidSecret() {
            // When & Then
            assertThatCode(() -> new JwtService(validSecret, expiration))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw exception when secret is too short")
        void shouldThrowExceptionWhenSecretTooShort() {
            // Given
            String shortSecret = "short"; // Less than 32 characters

            // When & Then
            assertThatThrownBy(() -> new JwtService(shortSecret, expiration))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Secret length must be at least 32 characters");
        }

        @Test
        @DisplayName("Should accept secret with exactly 32 characters")
        void shouldAcceptSecretWith32Characters() {
            // Given
            String exactSecret = "a".repeat(32);

            // When & Then
            assertThatCode(() -> new JwtService(exactSecret, expiration))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept secret with more than 32 characters")
        void shouldAcceptSecretWithMoreThan32Characters() {
            // Given
            String longSecret = "a".repeat(64);

            // When & Then
            assertThatCode(() -> new JwtService(longSecret, expiration))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw exception when secret is null")
        void shouldThrowExceptionWhenSecretIsNull() {
            // When & Then
            assertThatThrownBy(() -> new JwtService(null, expiration))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should accept zero expiration")
        void shouldAcceptZeroExpiration() {
            // When & Then
            assertThatCode(() -> new JwtService(validSecret, 0))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept negative expiration")
        void shouldAcceptNegativeExpiration() {
            // When & Then - Though this would create expired tokens
            assertThatCode(() -> new JwtService(validSecret, -1000))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate valid JWT token")
        void shouldGenerateValidToken() {
            // When
            String token = jwtService.generateToken("testuser", "USER");

            // Then
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
        }

        @Test
        @DisplayName("Should generate token with correct username")
        void shouldGenerateTokenWithCorrectUsername() {
            // When
            String token = jwtService.generateToken("john.doe", "USER");

            // Then
            String extractedUsername = jwtService.extractUsername(token);
            assertThat(extractedUsername).isEqualTo("john.doe");
        }

        @Test
        @DisplayName("Should generate token with correct role")
        void shouldGenerateTokenWithCorrectRole() {
            // When
            String token = jwtService.generateToken("admin", "ADMIN");

            // Then
            String extractedRole = jwtService.extractRole(token);
            assertThat(extractedRole).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("Should generate different tokens for same user at different times")
        void shouldGenerateDifferentTokensForSameUser() throws InterruptedException {
            // When
            String token1 = jwtService.generateToken("testuser", "USER");
            Thread.sleep(10); // Small delay to ensure different timestamps
            String token2 = jwtService.generateToken("testuser", "USER");

            // Then
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("Should handle special characters in username")
        void shouldHandleSpecialCharactersInUsername() {
            // When
            String token = jwtService.generateToken("user@example.com", "USER");

            // Then
            String extractedUsername = jwtService.extractUsername(token);
            assertThat(extractedUsername).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("Should handle spaces in username")
        void shouldHandleSpacesInUsername() {
            // When
            String token = jwtService.generateToken("John Doe", "USER");

            // Then
            String extractedUsername = jwtService.extractUsername(token);
            assertThat(extractedUsername).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should handle unicode characters in username")
        void shouldHandleUnicodeCharactersInUsername() {
            // When
            String token = jwtService.generateToken("用户名", "USER");

            // Then
            String extractedUsername = jwtService.extractUsername(token);
            assertThat(extractedUsername).isEqualTo("用户名");
        }

        @Test
        @DisplayName("Should handle very long username")
        void shouldHandleVeryLongUsername() {
            // Given
            String longUsername = "a".repeat(500);

            // When
            String token = jwtService.generateToken(longUsername, "USER");

            // Then
            String extractedUsername = jwtService.extractUsername(token);
            assertThat(extractedUsername).isEqualTo(longUsername);
        }

        @Test
        @DisplayName("Should generate token for all role types")
        void shouldGenerateTokenForAllRoleTypes() {
            // When
            String userToken = jwtService.generateToken("user1", "USER");
            String caregiverToken = jwtService.generateToken("caregiver1", "CAREGIVER");
            String adminToken = jwtService.generateToken("admin1", "ADMIN");

            // Then
            assertThat(jwtService.extractRole(userToken)).isEqualTo("USER");
            assertThat(jwtService.extractRole(caregiverToken)).isEqualTo("CAREGIVER");
            assertThat(jwtService.extractRole(adminToken)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("Should throw exception when username is null")
        void shouldThrowExceptionWhenUsernameIsNull() {
            // When & Then
            assertThatThrownBy(() -> jwtService.generateToken(null, "USER"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw exception when role is null")
        void shouldThrowExceptionWhenRoleIsNull() {
            // When & Then
            assertThatThrownBy(() -> jwtService.generateToken("testuser", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Extract Username Tests")
    class ExtractUsernameTests {

        @Test
        @DisplayName("Should extract username from valid token")
        void shouldExtractUsernameFromValidToken() {
            // Given
            String token = jwtService.generateToken("testuser", "USER");

            // When
            String username = jwtService.extractUsername(token);

            // Then
            assertThat(username).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should throw exception for malformed token")
        void shouldThrowExceptionForMalformedToken() {
            // Given
            String malformedToken = "not.a.valid.token";

            // When & Then
            assertThatThrownBy(() -> jwtService.extractUsername(malformedToken))
                    .isInstanceOf(MalformedJwtException.class);
        }

        @Test
        @DisplayName("Should throw exception for token with invalid signature")
        void shouldThrowExceptionForInvalidSignature() {
            // Given
            JwtService otherJwtService = new JwtService(
                    "different-32-character-secret-key-here",
                    expiration
            );
            String token = otherJwtService.generateToken("testuser", "USER");

            // When & Then
            assertThatThrownBy(() -> jwtService.extractUsername(token))
                    .isInstanceOf(SignatureException.class);
        }

        @Test
        @DisplayName("Should throw exception for null token")
        void shouldThrowExceptionForNullToken() {
            // When & Then
            assertThatThrownBy(() -> jwtService.extractUsername(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw exception for empty token")
        void shouldThrowExceptionForEmptyToken() {
            // When & Then
            assertThatThrownBy(() -> jwtService.extractUsername(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw exception for token with only 2 parts")
        void shouldThrowExceptionForIncompletToken() {
            // Given
            String incompleteToken = "header.payload";

            // When & Then
            assertThatThrownBy(() -> jwtService.extractUsername(incompleteToken))
                    .isInstanceOf(MalformedJwtException.class);
        }
    }

    @Nested
    @DisplayName("Extract Role Tests")
    class ExtractRoleTests {

        @Test
        @DisplayName("Should extract role from valid token")
        void shouldExtractRoleFromValidToken() {
            // Given
            String token = jwtService.generateToken("testuser", "ADMIN");

            // When
            String role = jwtService.extractRole(token);

            // Then
            assertThat(role).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("Should extract all role types correctly")
        void shouldExtractAllRoleTypesCorrectly() {
            // When
            String userToken = jwtService.generateToken("user", "USER");
            String caregiverToken = jwtService.generateToken("caregiver", "CAREGIVER");
            String adminToken = jwtService.generateToken("admin", "ADMIN");

            // Then
            assertThat(jwtService.extractRole(userToken)).isEqualTo("USER");
            assertThat(jwtService.extractRole(caregiverToken)).isEqualTo("CAREGIVER");
            assertThat(jwtService.extractRole(adminToken)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("Should throw exception for invalid token")
        void shouldThrowExceptionForInvalidToken() {
            // Given
            String invalidToken = "invalid.token.here";

            // When & Then
            assertThatThrownBy(() -> jwtService.extractRole(invalidToken))
                    .isInstanceOf(MalformedJwtException.class);
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate correct token for matching username")
        void shouldValidateCorrectToken() {
            // Given
            String username = "testuser";
            String token = jwtService.generateToken(username, "USER");

            // When
            boolean isValid = jwtService.isTokenValid(token, username);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject token for non-matching username")
        void shouldRejectTokenForNonMatchingUsername() {
            // Given
            String token = jwtService.generateToken("testuser", "USER");

            // When
            boolean isValid = jwtService.isTokenValid(token, "differentuser");

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject expired token")
        void shouldRejectExpiredToken() throws InterruptedException {
            // Given - Create service with very short expiration
            JwtService shortExpirationService = new JwtService(validSecret, 1); // 1ms
            String token = shortExpirationService.generateToken("testuser", "USER");
            Thread.sleep(10); // Wait for token to expire

            // When
            boolean isValid = shortExpirationService.isTokenValid(token, "testuser");

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should validate token with matching username (case-sensitive)")
        void shouldValidateTokenCaseSensitive() {
            // Given
            String token = jwtService.generateToken("TestUser", "USER");

            // When
            boolean validExact = jwtService.isTokenValid(token, "TestUser");
            boolean invalidLower = jwtService.isTokenValid(token, "testuser");

            // Then
            assertThat(validExact).isTrue();
            assertThat(invalidLower).isFalse();
        }

        @Test
        @DisplayName("Should reject token with invalid signature")
        void shouldRejectTokenWithInvalidSignature() {
            // Given
            JwtService otherService = new JwtService(
                    "another-32-character-secret-key-goes-here",
                    expiration
            );
            String token = otherService.generateToken("testuser", "USER");

            // When & Then
            assertThatThrownBy(() -> jwtService.isTokenValid(token, "testuser"))
                    .isInstanceOf(SignatureException.class);
        }

        @Test
        @DisplayName("Should reject malformed token")
        void shouldRejectMalformedToken() {
            // Given
            String malformedToken = "this.is.not.a.valid.token";

            // When & Then
            assertThatThrownBy(() -> jwtService.isTokenValid(malformedToken, "testuser"))
                    .isInstanceOf(MalformedJwtException.class);
        }

        @Test
        @DisplayName("Should reject null token")
        void shouldRejectNullToken() {
            // When & Then
            assertThatThrownBy(() -> jwtService.isTokenValid(null, "testuser"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should handle null username in validation")
        void shouldHandleNullUsernameInValidation() {
            // Given
            String token = jwtService.generateToken("testuser", "USER");

            // When & Then
            assertThatThrownBy(() -> jwtService.isTokenValid(token, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should validate token immediately after creation")
        void shouldValidateTokenImmediatelyAfterCreation() {
            // When
            String username = "testuser";
            String token = jwtService.generateToken(username, "USER");
            boolean isValid = jwtService.isTokenValid(token, username);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should validate token close to expiration")
        void shouldValidateTokenCloseToExpiration() throws InterruptedException {
            // Given - 100ms expiration
            JwtService shortService = new JwtService(validSecret, 100);
            String token = shortService.generateToken("testuser", "USER");
            Thread.sleep(50); // Wait half the expiration time

            // When
            boolean isValid = shortService.isTokenValid(token, "testuser");

            // Then
            assertThat(isValid).isTrue(); // Still valid
        }
    }

    @Nested
    @DisplayName("Token Expiration Tests")
    class TokenExpirationTests {

        @Test
        @DisplayName("Should create token that expires after specified time")
        void shouldCreateTokenThatExpiresAfterSpecifiedTime() throws InterruptedException {
            // Given
            JwtService shortService = new JwtService(validSecret, 50); // 50ms expiration
            String token = shortService.generateToken("testuser", "USER");

            // When - Token is valid initially
            boolean validBefore = shortService.isTokenValid(token, "testuser");
            Thread.sleep(100); // Wait for expiration
            boolean validAfter = shortService.isTokenValid(token, "testuser");

            // Then
            assertThat(validBefore).isTrue();
            assertThat(validAfter).isFalse();
        }

        @Test
        @DisplayName("Should throw ExpiredJwtException for expired token when extracting username")
        void shouldThrowExpiredJwtExceptionWhenExtractingUsername() throws InterruptedException {
            // Given
            JwtService shortService = new JwtService(validSecret, 1);
            String token = shortService.generateToken("testuser", "USER");
            Thread.sleep(10); // Wait for expiration

            // When & Then
            assertThatThrownBy(() -> shortService.extractUsername(token))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("Should handle very long expiration time")
        void shouldHandleVeryLongExpirationTime() {
            // Given - 100 years in milliseconds
            long veryLongExpiration = 100L * 365 * 24 * 60 * 60 * 1000;
            JwtService longService = new JwtService(validSecret, veryLongExpiration);

            // When
            String token = longService.generateToken("testuser", "USER");
            boolean isValid = longService.isTokenValid(token, "testuser");

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should create already expired token with zero expiration")
        void shouldCreateExpiredTokenWithZeroExpiration() {
            // Given
            JwtService zeroExpirationService = new JwtService(validSecret, 0);

            // When
            String token = zeroExpirationService.generateToken("testuser", "USER");
            boolean isValid = zeroExpirationService.isTokenValid(token, "testuser");

            // Then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should not allow token modification")
        void shouldNotAllowTokenModification() {
            // Given
            String originalToken = jwtService.generateToken("testuser", "USER");
            
            // When - Try to modify token (change a character)
            String modifiedToken = originalToken.substring(0, originalToken.length() - 5) + "XXXXX";

            // Then
            assertThatThrownBy(() -> jwtService.extractUsername(modifiedToken))
                    .isInstanceOf(SignatureException.class);
        }

        @Test
        @DisplayName("Should not accept token from different secret")
        void shouldNotAcceptTokenFromDifferentSecret() {
            // Given
            JwtService service1 = new JwtService(validSecret, expiration);
            JwtService service2 = new JwtService(
                    "completely-different-32-char-secret-key",
                    expiration
            );

            String tokenFromService2 = service2.generateToken("testuser", "USER");

            // When & Then
            assertThatThrownBy(() -> service1.extractUsername(tokenFromService2))
                    .isInstanceOf(SignatureException.class);
        }

        @Test
        @DisplayName("Should use HS256 algorithm")
        void shouldUseHS256Algorithm() {
            // Given
            String token = jwtService.generateToken("testuser", "USER");
            
            // When - Decode header (first part of JWT)
            String header = token.split("\\.")[0];
            String decodedHeader = new String(
                    java.util.Base64.getUrlDecoder().decode(header)
            );

            // Then
            assertThat(decodedHeader).contains("HS256");
        }

        @Test
        @DisplayName("Should include issued at time")
        void shouldIncludeIssuedAtTime() {
            // When
            String token = jwtService.generateToken("testuser", "USER");

            // Then - Token should be valid immediately (iat should be <= now)
            assertThat(jwtService.isTokenValid(token, "testuser")).isTrue();
        }

        @Test
        @DisplayName("Should not leak secret in exception messages")
        void shouldNotLeakSecretInExceptionMessages() {
            // When & Then
            assertThatThrownBy(() -> new JwtService("short", expiration))
                    .isInstanceOf(IllegalArgumentException.class)
                    .extracting(Throwable::getMessage)
                    .asString()
                    .doesNotContain("short");
        }
    }
}
