package projects.caregiver_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projects.caregiver_backend.dtos.request.CaregiverFilterRequest;
import projects.caregiver_backend.dtos.request.CaregiverOnboardingRequest;
import projects.caregiver_backend.dtos.response.CaregiverResponse;
import projects.caregiver_backend.model.Caregiver;
import projects.caregiver_backend.model.OnboardingStatus;
import projects.caregiver_backend.model.User;
import projects.caregiver_backend.repositories.AvailabilityRepository;
import projects.caregiver_backend.repositories.CaregiverRepository;
import projects.caregiver_backend.repositories.ReviewRepository;
import projects.caregiver_backend.repositories.UserRepository;
import projects.caregiver_backend.repositories.projections.CaregiverRatingView;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CaregiverService {

    private final CaregiverRepository caregiverRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final AvailabilityRepository availabilityRepository;


    @Transactional
    public CaregiverResponse onboardCaregiver(
            String username,
            CaregiverOnboardingRequest request
    ) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (caregiverRepository.existsByUser(user)) {
            throw new IllegalStateException("Already onboarded as caregiver");
        }

        Caregiver caregiver = new Caregiver();
        caregiver.setUser(user);
        caregiver.setFullName(request.getFullName());
        caregiver.setCity(request.getCity());
        caregiver.setNeighborhood(request.getNeighborhood());
        caregiver.setPhone(request.getPhone());
        caregiver.setBio(request.getBio());
        caregiver.setOnboardingStatus(OnboardingStatus.PENDING);

        Caregiver saved = caregiverRepository.save(caregiver);

        return new CaregiverResponse(
                saved.getId(),
                saved.getFullName(),
                saved.getCity(),
                saved.getNeighborhood(),
                saved.getPhone(),
                saved.getBio(),
                0.0,   // averageRating
                0L     // reviewCount
        );
    }


    public List<CaregiverResponse> browseCaregivers(
            String city,
            String neighborhood
    ) {

        List<Caregiver> caregivers =
                caregiverRepository.findByCityAndNeighborhoodAndVerifiedTrue(city, neighborhood);

        Map<UUID, CaregiverRatingView> ratingMap =
                reviewRepository.fetchCaregiverRatings()
                        .stream()
                        .collect(Collectors.toMap(
                                CaregiverRatingView::getCaregiverId,
                                r -> r
                        ));

        return caregivers.stream()
                .map(caregiver -> {
                    CaregiverRatingView rating =
                            ratingMap.get(caregiver.getId());

                    return new CaregiverResponse(
                            caregiver.getId(),
                            caregiver.getFullName(),
                            caregiver.getCity(),
                            caregiver.getNeighborhood(),
                            caregiver.getPhone(),
                            caregiver.getBio(),
                            rating != null ? rating.getAverageRating() : 0.0,
                            rating != null ? rating.getReviewCount() : 0L
                    );
                })
                .toList();
    }

    public List<CaregiverResponse> filterCaregivers(
            CaregiverFilterRequest request
    ) {

        List<Caregiver> caregivers =
                caregiverRepository.filterCaregivers(
                        request.city(),
                        request.neighborhood(),
                        request.minPrice(),
                        request.maxPrice()
                );

        // Availability filter
        if (request.availableDate() != null) {
            List<UUID> availableIds =
                    availabilityRepository.findAvailableCaregiverIds(
                            request.availableDate()
                    );

            caregivers = caregivers.stream()
                    .filter(c -> availableIds.contains(c.getId()))
                    .toList();
        }

        // Ratings (bulk fetch)
        Map<UUID, CaregiverRatingView> ratings =
                reviewRepository.fetchCaregiverRatings()
                        .stream()
                        .collect(Collectors.toMap(
                                CaregiverRatingView::getCaregiverId,
                                r -> r
                        ));

        return caregivers.stream()
                .filter(c -> {
                    CaregiverRatingView r = ratings.get(c.getId());
                    return request.minRating() == null ||
                            (r != null && r.getAverageRating() >= request.minRating());
                })
                .map(c -> {
                    CaregiverRatingView r = ratings.get(c.getId());

                    return new CaregiverResponse(
                            c.getId(),
                            c.getFullName(),
                            c.getCity(),
                            c.getNeighborhood(),
                            c.getPhone(),
                            c.getBio(),
                            r != null ? r.getAverageRating() : 0.0,
                            r != null ? r.getReviewCount() : 0L
                    );
                })
                .toList();
    }

}
