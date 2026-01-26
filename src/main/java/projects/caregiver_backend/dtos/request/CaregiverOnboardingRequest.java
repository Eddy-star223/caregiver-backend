package projects.caregiver_backend.dtos.request;

import lombok.Getter;

@Getter
public class CaregiverOnboardingRequest {

    private String fullName;
    private String city;
    private String neighborhood;
    private String phone;
    private String bio;
}
