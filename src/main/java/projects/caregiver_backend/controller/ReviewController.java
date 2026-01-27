package projects.caregiver_backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import projects.caregiver_backend.dtos.request.ReviewRequest;
import projects.caregiver_backend.dtos.response.ReviewResponse;
import projects.caregiver_backend.service.ReviewService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/{caregiverId}")
    public ReviewResponse createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String caregiverId,
            @Valid @RequestBody ReviewRequest request
    ) {
        return reviewService.createReview(
                userDetails.getUsername(),
                caregiverId,
                request
        );
    }

    @GetMapping("/{caregiverId}")
    public List<ReviewResponse> getCaregiverReviews(
            @PathVariable String caregiverId
    ) {
        return reviewService.getReviewsForCaregiver(caregiverId);
    }
}
