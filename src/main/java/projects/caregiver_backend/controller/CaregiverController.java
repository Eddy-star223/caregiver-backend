package projects.caregiver_backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import projects.caregiver_backend.dtos.request.CaregiverFilterRequest;
import projects.caregiver_backend.dtos.request.CaregiverOnboardingRequest;
import projects.caregiver_backend.dtos.response.CaregiverResponse;
import projects.caregiver_backend.model.Caregiver;
import projects.caregiver_backend.model.OnboardingStatus;
import projects.caregiver_backend.repositories.CaregiverRepository;
import projects.caregiver_backend.service.CaregiverService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/caregivers")
@RequiredArgsConstructor
public class CaregiverController {

    private final CaregiverService caregiverService;
    private final CaregiverRepository caregiverRepository;

    @PostMapping("/onboard")
    public ResponseEntity<?> onboardCaregiver(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CaregiverOnboardingRequest request
    ) {
        CaregiverResponse caregiver = caregiverService.onboardCaregiver(
                userDetails.getUsername(),
                request
        );
        return ResponseEntity.ok(caregiver);
    }

    @PutMapping("/admin/caregivers/{id}/approve")
    public ResponseEntity<String> approveCaregiver(@PathVariable UUID id) {

        Caregiver caregiver = caregiverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Caregiver not found"));

        caregiver.setOnboardingStatus(OnboardingStatus.VERIFIED);
        caregiverRepository.save(caregiver);

        return ResponseEntity.ok("Caregiver approved");
    }

    @GetMapping("/browse")
    public List<CaregiverResponse> browseCaregivers(
            @RequestParam String city,
            @RequestParam(required = false) String neighborhood
    ) {
        return caregiverService.browseCaregivers(city, neighborhood);
    }

    @PostMapping("/search")
    public ResponseEntity<List<CaregiverResponse>> searchCaregivers(
            @RequestBody CaregiverFilterRequest request
    ) {
        return ResponseEntity.ok(
                caregiverService.filterCaregivers(request)
        );
    }

}
