package projects.caregiver_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projects.caregiver_backend.dtos.request.CaregiverOnboardingRequest;
import projects.caregiver_backend.dtos.response.CaregiverResponse;
import projects.caregiver_backend.model.Caregiver;
import projects.caregiver_backend.model.OnboardingStatus;
import projects.caregiver_backend.model.User;
import projects.caregiver_backend.repositories.CaregiverRepository;
import projects.caregiver_backend.repositories.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CaregiverService {

    private final CaregiverRepository caregiverRepository;
    private final UserRepository userRepository;

    @Transactional
    public CaregiverResponse onboardCaregiver(
            String username,
            CaregiverOnboardingRequest request
    ) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (caregiverRepository.existsByUser(user)) {
            throw new RuntimeException("Already onboarded as caregiver");
        }

        Caregiver caregiver = new Caregiver();
        caregiver.setUser(user);
        caregiver.setFullName(request.getFullName());
        caregiver.setCity(request.getCity());
        caregiver.setNeighborhood(request.getNeighborhood());
        caregiver.setPhone(request.getPhone());
        caregiver.setBio(request.getBio());

        if (caregiver.getOnboardingStatus() != OnboardingStatus.PENDING) {
            throw new IllegalStateException("Caregiver already processed");
        }

        Caregiver saved = caregiverRepository.save(caregiver);

        return new CaregiverResponse(
                saved.getId(),
                saved.getFullName(),
                saved.getCity(),
                saved.getNeighborhood(),
                saved.getPhone(),
                saved.getBio()
        );
    }

    public List<CaregiverResponse> browseCaregivers(
            String city,
            String neighborhood
    ) {

        List<Caregiver> caregivers;

        if (neighborhood != null && !neighborhood.isBlank()) {
            caregivers = caregiverRepository
                    .findByCityAndNeighborhoodAndOnboardingStatus(
                            city,
                            neighborhood,
                            OnboardingStatus.VERIFIED
                    );
        } else {
            caregivers = caregiverRepository
                    .findByCityAndOnboardingStatus(
                            city,
                            OnboardingStatus.VERIFIED
                    );
        }

        return caregivers.stream()
                .map(c -> new CaregiverResponse(
                        c.getId(),
                        c.getFullName(),
                        c.getCity(),
                        c.getNeighborhood(),
                        c.getPhone(),
                        c.getBio()
                ))
                .toList();
    }

}
