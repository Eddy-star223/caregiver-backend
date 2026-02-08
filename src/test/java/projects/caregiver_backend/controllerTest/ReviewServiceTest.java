package projects.caregiver_backend.controllerTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import projects.caregiver_backend.dtos.request.ReviewRequest;
import projects.caregiver_backend.dtos.response.ReviewResponse;
import projects.caregiver_backend.model.Caregiver;
import projects.caregiver_backend.model.Review;
import projects.caregiver_backend.model.Role;
import projects.caregiver_backend.model.User;
import projects.caregiver_backend.repositories.CaregiverRepository;
import projects.caregiver_backend.repositories.ReviewRepository;
import projects.caregiver_backend.repositories.UserRepository;
import projects.caregiver_backend.service.ReviewService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Tests")
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CaregiverRepository caregiverRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User reviewer;
    private User caregiverUser;
    private Caregiver caregiver;
    private ReviewRequest validReviewRequest;

    @BeforeEach
    void setUp() {
        reviewer = new User();
        reviewer.setId(UUID.randomUUID());
        reviewer.setUsername("reviewer");
        reviewer.setEmail("reviewer@example.com");
        reviewer.setRole(Role.USER);

        caregiverUser = new User();
        caregiverUser.setId(UUID.randomUUID());
        caregiverUser.setUsername("caregiver1");
        caregiverUser.setRole(Role.CAREGIVER);

        caregiver = new Caregiver();
        caregiver.setId(UUID.randomUUID());
        caregiver.setUser(caregiverUser);
        caregiver.setFullName("John Doe");

        validReviewRequest = new ReviewRequest(5, "Excellent service!");
    }

    @Nested
    @DisplayName("Create Review Tests")
    class CreateReviewTests {

        @Test
        @DisplayName("Should successfully create review with valid data")
        void shouldCreateReviewSuccessfully() {
            // Given
            when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
            when(caregiverRepository.findById(caregiver.getId()))
                    .thenReturn(Optional.of(caregiver));
            when(reviewRepository.existsByUserAndCaregiver(reviewer, caregiver))
                    .thenReturn(false);

            Review savedReview = Review.builder()
                    .id(UUID.randomUUID())
                    .user(reviewer)
                    .caregiver(caregiver)
                    .rating(5)
                    .comment("Excellent service!")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

            // When
            ReviewResponse response = reviewService.createReview(
                    "reviewer",
                    caregiver.getId().toString(),
                    validReviewRequest
            );

            // Then
            assertThat(response).isNotNull();
            assertThat(response.reviewerUsername()).isEqualTo("reviewer");
            assertThat(response.rating()).isEqualTo(5);
            assertThat(response.comment()).isEqualTo("Excellent service!");

            verify(userRepository).findByUsername("reviewer");
            verify(caregiverRepository).findById(caregiver.getId());
            verify(reviewRepository).existsByUserAndCaregiver(reviewer, caregiver);
            verify(reviewRepository).save(any(Review.class));
        }

        @Test
        @DisplayName("Should create review with minimum rating (1)")
        void shouldCreateReviewWithMinimumRating() {
            // Given
            ReviewRequest minRatingRequest = new ReviewRequest(1, "Poor service");
            when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(reviewRepository.existsByUserAndCaregiver(any(), any())).thenReturn(false);

            Review savedReview = Review.builder()
                    .id(UUID.randomUUID())
                    .user(reviewer)
                    .caregiver(caregiver)
                    .rating(1)
                    .comment("Poor service")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

            // When
            ReviewResponse response = reviewService.createReview(
                    "reviewer",
                    caregiver.getId().toString(),
                    minRatingRequest
            );

            // Then
            assertThat(response.rating()).isEqualTo(1);
            verify(reviewRepository).save(argThat(review ->
                    review.getRating() == 1
            ));
        }

        @Test
        @DisplayName("Should create review with maximum rating (5)")
        void shouldCreateReviewWithMaximumRating() {
            // Given
            ReviewRequest maxRatingRequest = new ReviewRequest(5, "Outstanding!");
            when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(reviewRepository.existsByUserAndCaregiver(any(), any())).thenReturn(false);

            Review savedReview = Review.builder()
                    .id(UUID.randomUUID())
                    .user(reviewer)
                    .caregiver(caregiver)
                    .rating(5)
                    .comment("Outstanding!")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

            // When
            ReviewResponse response = reviewService.createReview(
                    "reviewer",
                    caregiver.getId().toString(),
                    maxRatingRequest
            );

            // Then
            assertThat(response.rating()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should create review with null comment")
        void shouldCreateReviewWithNullComment() {
            // Given
            ReviewRequest nullCommentRequest = new ReviewRequest(4, null);
            when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(reviewRepository.existsByUserAndCaregiver(any(), any())).thenReturn(false);

            Review savedReview = Review.builder()
                    .id(UUID.randomUUID())
                    .user(reviewer)
                    .caregiver(caregiver)
                    .rating(4)
                    .comment(null)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

            // When
            ReviewResponse response = reviewService.createReview(
                    "reviewer",
                    caregiver.getId().toString(),
                    nullCommentRequest
            );

            // Then
            assertThat(response.comment()).isNull();
        }

        @Test
        @DisplayName("Should create review with empty comment")
        void shouldCreateReviewWithEmptyComment() {
            // Given
            ReviewRequest emptyCommentRequest = new ReviewRequest(3, "");
            when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(reviewRepository.existsByUserAndCaregiver(any(), any())).thenReturn(false);

            Review savedReview = Review.builder()
                    .id(UUID.randomUUID())
                    .user(reviewer)
                    .caregiver(caregiver)
                    .rating(3)
                    .comment("")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

            // When
            ReviewResponse response = reviewService.createReview(
                    "reviewer",
                    caregiver.getId().toString(),
                    emptyCommentRequest
            );

            // Then
            assertThat(response.comment()).isEmpty();
        }

        @Test
        @DisplayName("Should create review with long comment (500 chars)")
        void shouldCreateReviewWithLongComment() {
            // Given
            String longComment = "a".repeat(500);
            ReviewRequest longCommentRequest = new ReviewRequest(5, longComment);
            
            when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(reviewRepository.existsByUserAndCaregiver(any(), any())).thenReturn(false);

            Review savedReview = Review.builder()
                    .id(UUID.randomUUID())
                    .user(reviewer)
                    .caregiver(caregiver)
                    .rating(5)
                    .comment(longComment)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

            // When
            ReviewResponse response = reviewService.createReview(
                    "reviewer",
                    caregiver.getId().toString(),
                    longCommentRequest
            );

            // Then
            assertThat(response.comment()).hasSize(500);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reviewService.createReview(
                    "nonexistent",
                    caregiver.getId().toString(),
                    validReviewRequest
            ))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");

            verify(caregiverRepository, never()).findById(any());
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when caregiver not found")
        void shouldThrowExceptionWhenCaregiverNotFound() {
            // Given
            when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reviewService.createReview(
                    "reviewer",
                    caregiver.getId().toString(),
                    validReviewRequest
            ))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Caregiver not found");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when trying to review self")
        void shouldThrowExceptionWhenReviewingSelf() {
            // Given - Reviewer IS the caregiver
            caregiver.setUser(reviewer);
            
            when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));

            // When & Then
            assertThatThrownBy(() -> reviewService.createReview(
                    "reviewer",
                    caregiver.getId().toString(),
                    validReviewRequest
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("You cannot review yourself");

            verify(reviewRepository, never()).existsByUserAndCaregiver(any(), any());
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when duplicate review exists")
        void shouldThrowExceptionWhenDuplicateReviewExists() {
            // Given
            when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(reviewRepository.existsByUserAndCaregiver(reviewer, caregiver)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> reviewService.createReview(
                    "reviewer",
                    caregiver.getId().toString(),
                    validReviewRequest
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("You already reviewed this caregiver");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception with invalid caregiver ID format")
        void shouldThrowExceptionWithInvalidCaregiverIdFormat() {
            // Given
            when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));

            // When & Then
            assertThatThrownBy(() -> reviewService.createReview(
                    "reviewer",
                    "invalid-uuid",
                    validReviewRequest
            ))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(caregiverRepository, never()).findById(any());
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should set createdAt timestamp")
        void shouldSetCreatedAtTimestamp() {
            // Given
            when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(reviewRepository.existsByUserAndCaregiver(any(), any())).thenReturn(false);
            when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArguments()[0]);

            // When
            reviewService.createReview(
                    "reviewer",
                    caregiver.getId().toString(),
                    validReviewRequest
            );

            // Then
            verify(reviewRepository).save(argThat(review ->
                    review.getCreatedAt() != null
            ));
        }

        @Test
        @DisplayName("Should handle special characters in comment")
        void shouldHandleSpecialCharactersInComment() {
            // Given
            ReviewRequest specialCharsRequest = new ReviewRequest(
                    5,
                    "Great service! 😊 Very professional. Rating: 5/5 ★★★★★"
            );

            when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(reviewRepository.existsByUserAndCaregiver(any(), any())).thenReturn(false);

            Review savedReview = Review.builder()
                    .id(UUID.randomUUID())
                    .user(reviewer)
                    .caregiver(caregiver)
                    .rating(5)
                    .comment(specialCharsRequest.comment())
                    .createdAt(LocalDateTime.now())
                    .build();

            when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

            // When
            ReviewResponse response = reviewService.createReview(
                    "reviewer",
                    caregiver.getId().toString(),
                    specialCharsRequest
            );

            // Then
            assertThat(response.comment()).contains("😊", "★★★★★");
        }
    }

    @Nested
    @DisplayName("Get Reviews For Caregiver Tests")
    class GetReviewsTests {

        @Test
        @DisplayName("Should return all reviews for caregiver")
        void shouldReturnAllReviewsForCaregiver() {
            // Given
            Review review1 = Review.builder()
                    .id(UUID.randomUUID())
                    .user(reviewer)
                    .caregiver(caregiver)
                    .rating(5)
                    .comment("Excellent!")
                    .createdAt(LocalDateTime.now())
                    .build();

            User anotherReviewer = new User();
            anotherReviewer.setUsername("reviewer2");

            Review review2 = Review.builder()
                    .id(UUID.randomUUID())
                    .user(anotherReviewer)
                    .caregiver(caregiver)
                    .rating(4)
                    .comment("Good service")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(caregiverRepository.findById(caregiver.getId()))
                    .thenReturn(Optional.of(caregiver));
            when(reviewRepository.findByCaregiver(caregiver))
                    .thenReturn(Arrays.asList(review1, review2));

            // When
            List<ReviewResponse> responses = reviewService.getReviewsForCaregiver(
                    caregiver.getId().toString()
            );

            // Then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).reviewerUsername()).isEqualTo("reviewer");
            assertThat(responses.get(0).rating()).isEqualTo(5);
            assertThat(responses.get(1).reviewerUsername()).isEqualTo("reviewer2");
            assertThat(responses.get(1).rating()).isEqualTo(4);

            verify(caregiverRepository).findById(caregiver.getId());
            verify(reviewRepository).findByCaregiver(caregiver);
        }

        @Test
        @DisplayName("Should return empty list when no reviews exist")
        void shouldReturnEmptyListWhenNoReviewsExist() {
            // Given
            when(caregiverRepository.findById(caregiver.getId()))
                    .thenReturn(Optional.of(caregiver));
            when(reviewRepository.findByCaregiver(caregiver))
                    .thenReturn(Arrays.asList());

            // When
            List<ReviewResponse> responses = reviewService.getReviewsForCaregiver(
                    caregiver.getId().toString()
            );

            // Then
            assertThat(responses).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when caregiver not found")
        void shouldThrowExceptionWhenCaregiverNotFoundForGetReviews() {
            // Given
            when(caregiverRepository.findById(caregiver.getId()))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reviewService.getReviewsForCaregiver(
                    caregiver.getId().toString()
            ))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Caregiver not found");

            verify(reviewRepository, never()).findByCaregiver(any());
        }

        @Test
        @DisplayName("Should throw exception with invalid caregiver ID format")
        void shouldThrowExceptionWithInvalidIdFormatForGetReviews() {
            // When & Then
            assertThatThrownBy(() -> reviewService.getReviewsForCaregiver("invalid-uuid"))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(caregiverRepository, never()).findById(any());
            verify(reviewRepository, never()).findByCaregiver(any());
        }

        @Test
        @DisplayName("Should preserve review order from repository")
        void shouldPreserveReviewOrder() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            Review oldReview = Review.builder()
                    .id(UUID.randomUUID())
                    .user(reviewer)
                    .caregiver(caregiver)
                    .rating(5)
                    .comment("Old review")
                    .createdAt(now.minusDays(5))
                    .build();

            Review newReview = Review.builder()
                    .id(UUID.randomUUID())
                    .user(reviewer)
                    .caregiver(caregiver)
                    .rating(4)
                    .comment("New review")
                    .createdAt(now)
                    .build();

            when(caregiverRepository.findById(caregiver.getId()))
                    .thenReturn(Optional.of(caregiver));
            when(reviewRepository.findByCaregiver(caregiver))
                    .thenReturn(Arrays.asList(newReview, oldReview));

            // When
            List<ReviewResponse> responses = reviewService.getReviewsForCaregiver(
                    caregiver.getId().toString()
            );

            // Then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).comment()).isEqualTo("New review");
            assertThat(responses.get(1).comment()).isEqualTo("Old review");
        }

        @Test
        @DisplayName("Should handle reviews with null comments")
        void shouldHandleReviewsWithNullComments() {
            // Given
            Review reviewWithNullComment = Review.builder()
                    .id(UUID.randomUUID())
                    .user(reviewer)
                    .caregiver(caregiver)
                    .rating(4)
                    .comment(null)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(caregiverRepository.findById(caregiver.getId()))
                    .thenReturn(Optional.of(caregiver));
            when(reviewRepository.findByCaregiver(caregiver))
                    .thenReturn(Arrays.asList(reviewWithNullComment));

            // When
            List<ReviewResponse> responses = reviewService.getReviewsForCaregiver(
                    caregiver.getId().toString()
            );

            // Then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).comment()).isNull();
        }

        @Test
        @DisplayName("Should handle large number of reviews")
        void shouldHandleLargeNumberOfReviews() {
            // Given
            List<Review> manyReviews = new java.util.ArrayList<>();
            for (int i = 0; i < 100; i++) {
                User tempUser = new User();
                tempUser.setUsername("user" + i);
                
                Review review = Review.builder()
                        .id(UUID.randomUUID())
                        .user(tempUser)
                        .caregiver(caregiver)
                        .rating(i % 5 + 1) // Ratings 1-5
                        .comment("Review " + i)
                        .createdAt(LocalDateTime.now().minusDays(i))
                        .build();
                manyReviews.add(review);
            }

            when(caregiverRepository.findById(caregiver.getId()))
                    .thenReturn(Optional.of(caregiver));
            when(reviewRepository.findByCaregiver(caregiver))
                    .thenReturn(manyReviews);

            // When
            List<ReviewResponse> responses = reviewService.getReviewsForCaregiver(
                    caregiver.getId().toString()
            );

            // Then
            assertThat(responses).hasSize(100);
        }
    }
}
