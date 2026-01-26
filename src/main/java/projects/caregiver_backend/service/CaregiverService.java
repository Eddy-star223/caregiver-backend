package projects.caregiver_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projects.caregiver_backend.dtos.request.CaregiverOnboardingRequest;
import projects.caregiver_backend.dtos.response.CaregiverResponse;
import projects.caregiver_backend.model.Caregiver;
import projects.caregiver_backend.model.User;
import projects.caregiver_backend.repositories.CaregiverRepository;
import projects.caregiver_backend.repositories.UserRepository;

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
}
