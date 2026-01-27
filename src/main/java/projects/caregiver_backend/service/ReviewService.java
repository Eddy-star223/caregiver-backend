package projects.caregiver_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projects.caregiver_backend.dtos.request.ReviewRequest;
import projects.caregiver_backend.dtos.response.ReviewResponse;
import projects.caregiver_backend.model.*;

import projects.caregiver_backend.repositories.*;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final CaregiverRepository caregiverRepository;

    @Transactional
    public ReviewResponse createReview(
            String username,
            String caregiverId,
            ReviewRequest request
    ) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Caregiver caregiver = caregiverRepository.findById(
                java.util.UUID.fromString(caregiverId)
        ).orElseThrow(() -> new RuntimeException("Caregiver not found"));

        // Prevent self-review
        if (caregiver.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("You cannot review yourself");
        }

        // Prevent duplicate review
        if (reviewRepository.existsByUserAndCaregiver(user, caregiver)) {
            throw new IllegalStateException("You already reviewed this caregiver");
        }

        Review review = Review.builder()
                .user(user)
                .caregiver(caregiver)
                .rating(request.rating())
                .comment(request.comment())
                .createdAt(LocalDateTime.now())
                .build();

        Review saved = reviewRepository.save(review);

        return new ReviewResponse(
                saved.getId(),
                user.getUsername(),
                saved.getRating(),
                saved.getComment(),
                saved.getCreatedAt()
        );
    }

    public List<ReviewResponse> getReviewsForCaregiver(String caregiverId) {
        Caregiver caregiver = caregiverRepository.findById(
                java.util.UUID.fromString(caregiverId)
        ).orElseThrow(() -> new RuntimeException("Caregiver not found"));

        return reviewRepository.findByCaregiver(caregiver)
                .stream()
                .map(r -> new ReviewResponse(
                        r.getId(),
                        r.getUser().getUsername(),
                        r.getRating(),
                        r.getComment(),
                        r.getCreatedAt()
                ))
                .toList();
    }
}
