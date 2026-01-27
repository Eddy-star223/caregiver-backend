package projects.caregiver_backend.repositories.projections;

import java.util.UUID;

public interface CaregiverRatingView {

    UUID getCaregiverId();

    Double getAverageRating();

    Long getReviewCount();
}
