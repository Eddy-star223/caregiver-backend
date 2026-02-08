package projects.caregiver_backend.controllerTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import projects.caregiver_backend.dtos.request.BookingRequest;
import projects.caregiver_backend.dtos.response.BookingResponse;
import projects.caregiver_backend.model.*;
import projects.caregiver_backend.repositories.BookingRepository;
import projects.caregiver_backend.repositories.CaregiverRepository;
import projects.caregiver_backend.repositories.UserRepository;
import projects.caregiver_backend.service.BookingService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Tests")
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CaregiverRepository caregiverRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private User user;
    private Caregiver caregiver;
    private BookingRequest validBookingRequest;
    private Booking savedBooking;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setRole(Role.USER);

        User caregiverUser = new User();
        caregiverUser.setId(UUID.randomUUID());
        caregiverUser.setUsername("caregiver1");
        caregiverUser.setRole(Role.CAREGIVER);

        caregiver = new Caregiver();
        caregiver.setId(UUID.randomUUID());
        caregiver.setUser(caregiverUser);
        caregiver.setFullName("John Doe");
        caregiver.setOnboardingStatus(OnboardingStatus.VERIFIED);
        caregiver.setHourlyRate(new BigDecimal("50.00"));

        validBookingRequest = new BookingRequest(
                caregiver.getId(),
                LocalDate.now().plusDays(1),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        savedBooking = new Booking();
        savedBooking.setId(UUID.randomUUID());
        savedBooking.setUser(user);
        savedBooking.setCaregiver(caregiver);
        savedBooking.setDate(validBookingRequest.date());
        savedBooking.setStartTime(validBookingRequest.startTime());
        savedBooking.setEndTime(validBookingRequest.endTime());
        savedBooking.setStatus(BookingStatus.PENDING);
        savedBooking.setTotalAmount(new BigDecimal("400.00")); // 8 hours * 50
    }

    @Nested
    @DisplayName("Create Booking Tests")
    class CreateBookingTests {

        @Test
        @DisplayName("Should successfully create booking with valid data")
        void shouldCreateBookingSuccessfully() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(bookingRepository.existsByCaregiverAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                    any(), any(), any(), any())).thenReturn(false);
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            // When
            BookingResponse response = bookingService.createBooking("testuser", validBookingRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(savedBooking.getId());
            assertThat(response.caregiverId()).isEqualTo(caregiver.getId());
            assertThat(response.caregiverName()).isEqualTo("John Doe");
            assertThat(response.status()).isEqualTo(BookingStatus.PENDING);
            assertThat(response.price()).isEqualTo(new BigDecimal("400.00"));

            verify(userRepository).findByUsername("testuser");
            verify(caregiverRepository).findById(caregiver.getId());
            verify(bookingRepository).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should calculate total amount correctly for 8 hour booking")
        void shouldCalculateTotalAmountFor8Hours() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(bookingRepository.existsByCaregiverAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                    any(), any(), any(), any())).thenReturn(false);
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            // When
            bookingService.createBooking("testuser", validBookingRequest);

            // Then
            verify(bookingRepository).save(argThat(booking ->
                    booking.getTotalAmount().compareTo(new BigDecimal("400.00")) == 0
            ));
        }

        @Test
        @DisplayName("Should calculate total amount correctly for 1 hour booking")
        void shouldCalculateTotalAmountFor1Hour() {
            // Given
            BookingRequest oneHourRequest = new BookingRequest(
                    caregiver.getId(),
                    LocalDate.now().plusDays(1),
                    LocalTime.of(9, 0),
                    LocalTime.of(10, 0)
            );

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(bookingRepository.existsByCaregiverAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                    any(), any(), any(), any())).thenReturn(false);
            when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArguments()[0]);

            // When
            bookingService.createBooking("testuser", oneHourRequest);

            // Then
            verify(bookingRepository).save(argThat(booking ->
                    booking.getTotalAmount().compareTo(new BigDecimal("50.00")) == 0
            ));
        }

        @Test
        @DisplayName("Should calculate total amount correctly for partial hour booking")
        void shouldCalculateTotalAmountForPartialHour() {
            // Given - 1.5 hours (90 minutes)
            BookingRequest partialHourRequest = new BookingRequest(
                    caregiver.getId(),
                    LocalDate.now().plusDays(1),
                    LocalTime.of(9, 0),
                    LocalTime.of(10, 30)
            );

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(bookingRepository.existsByCaregiverAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                    any(), any(), any(), any())).thenReturn(false);
            when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArguments()[0]);

            // When
            bookingService.createBooking("testuser", partialHourRequest);

            // Then
            verify(bookingRepository).save(argThat(booking ->
                    booking.getTotalAmount().compareTo(new BigDecimal("75.00")) == 0
            ));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> bookingService.createBooking("nonexistent", validBookingRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");

            verify(userRepository).findByUsername("nonexistent");
            verify(caregiverRepository, never()).findById(any());
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when caregiver not found")
        void shouldThrowExceptionWhenCaregiverNotFound() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> bookingService.createBooking("testuser", validBookingRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Caregiver not found");

            verify(userRepository).findByUsername("testuser");
            verify(caregiverRepository).findById(caregiver.getId());
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when caregiver is not verified")
        void shouldThrowExceptionWhenCaregiverNotVerified() {
            // Given
            caregiver.setOnboardingStatus(OnboardingStatus.PENDING);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));

            // When & Then
            assertThatThrownBy(() -> bookingService.createBooking("testuser", validBookingRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Caregiver not approved");

            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when caregiver is rejected")
        void shouldThrowExceptionWhenCaregiverRejected() {
            // Given
            caregiver.setOnboardingStatus(OnboardingStatus.REJECTED);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));

            // When & Then
            assertThatThrownBy(() -> bookingService.createBooking("testuser", validBookingRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Caregiver not approved");
        }

        @Test
        @DisplayName("Should throw exception when time slot conflicts with existing booking")
        void shouldThrowExceptionWhenTimeSlotConflicts() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(bookingRepository.existsByCaregiverAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                    caregiver,
                    validBookingRequest.date(),
                    validBookingRequest.endTime(),
                    validBookingRequest.startTime()
            )).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> bookingService.createBooking("testuser", validBookingRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Time slot already booked");

            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should detect conflict for overlapping start time")
        void shouldDetectConflictForOverlappingStartTime() {
            // Given - New booking starts during existing booking
            BookingRequest overlappingRequest = new BookingRequest(
                    caregiver.getId(),
                    LocalDate.now().plusDays(1),
                    LocalTime.of(16, 0), // Overlaps with existing 9-17 booking
                    LocalTime.of(18, 0)
            );

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(bookingRepository.existsByCaregiverAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                    any(), any(), any(), any())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> bookingService.createBooking("testuser", overlappingRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Time slot already booked");
        }

        @Test
        @DisplayName("Should detect conflict for overlapping end time")
        void shouldDetectConflictForOverlappingEndTime() {
            // Given - New booking ends during existing booking
            BookingRequest overlappingRequest = new BookingRequest(
                    caregiver.getId(),
                    LocalDate.now().plusDays(1),
                    LocalTime.of(7, 0),
                    LocalTime.of(10, 0) // Overlaps with existing 9-17 booking
            );

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(bookingRepository.existsByCaregiverAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                    any(), any(), any(), any())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> bookingService.createBooking("testuser", overlappingRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Time slot already booked");
        }

        @Test
        @DisplayName("Should detect conflict for encompassing booking")
        void shouldDetectConflictForEncompassingBooking() {
            // Given - New booking encompasses existing booking
            BookingRequest encompassingRequest = new BookingRequest(
                    caregiver.getId(),
                    LocalDate.now().plusDays(1),
                    LocalTime.of(8, 0),
                    LocalTime.of(18, 0) // Encompasses existing 9-17 booking
            );

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(bookingRepository.existsByCaregiverAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                    any(), any(), any(), any())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> bookingService.createBooking("testuser", encompassingRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Time slot already booked");
        }

        @Test
        @DisplayName("Should allow booking on different date")
        void shouldAllowBookingOnDifferentDate() {
            // Given
            BookingRequest differentDateRequest = new BookingRequest(
                    caregiver.getId(),
                    LocalDate.now().plusDays(2), // Different date
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0)
            );

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(bookingRepository.existsByCaregiverAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                    any(), any(), any(), any())).thenReturn(false);
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            // When
            BookingResponse response = bookingService.createBooking("testuser", differentDateRequest);

            // Then
            assertThat(response).isNotNull();
            verify(bookingRepository).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should set booking status to PENDING")
        void shouldSetBookingStatusToPending() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(bookingRepository.existsByCaregiverAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                    any(), any(), any(), any())).thenReturn(false);
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            // When
            bookingService.createBooking("testuser", validBookingRequest);

            // Then
            verify(bookingRepository).save(argThat(booking ->
                    booking.getStatus() == BookingStatus.PENDING
            ));
        }

        @Test
        @DisplayName("Should set createdAt timestamp")
        void shouldSetCreatedAtTimestamp() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(bookingRepository.existsByCaregiverAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                    any(), any(), any(), any())).thenReturn(false);
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            // When
            bookingService.createBooking("testuser", validBookingRequest);

            // Then
            verify(bookingRepository).save(argThat(booking ->
                    booking.getCreatedAt() != null
            ));
        }

        @Test
        @DisplayName("Should handle midnight booking times")
        void shouldHandleMidnightBookingTimes() {
            // Given
            BookingRequest midnightRequest = new BookingRequest(
                    caregiver.getId(),
                    LocalDate.now().plusDays(1),
                    LocalTime.of(23, 0),
                    LocalTime.of(23, 59)
            );

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(bookingRepository.existsByCaregiverAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                    any(), any(), any(), any())).thenReturn(false);
            when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArguments()[0]);

            // When
            bookingService.createBooking("testuser", midnightRequest);

            // Then
            verify(bookingRepository).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should handle very high hourly rates")
        void shouldHandleVeryHighHourlyRates() {
            // Given
            caregiver.setHourlyRate(new BigDecimal("999.99"));
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(caregiverRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
            when(bookingRepository.existsByCaregiverAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                    any(), any(), any(), any())).thenReturn(false);
            when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArguments()[0]);

            // When
            bookingService.createBooking("testuser", validBookingRequest);

            // Then
            verify(bookingRepository).save(argThat(booking ->
                    booking.getTotalAmount().compareTo(new BigDecimal("7999.92")) == 0 // 8 hours * 999.99
            ));
        }
    }

    @Nested
    @DisplayName("Decide Booking Tests")
    class DecideBookingTests {

        private User caregiverUser;

        @BeforeEach
        void setUp() {
            caregiverUser = new User();
            caregiverUser.setId(UUID.randomUUID());
            caregiverUser.setUsername("caregiver1");
            caregiverUser.setRole(Role.CAREGIVER);

            caregiver.setUser(caregiverUser);
        }

        @Test
        @DisplayName("Should accept booking when caregiver approves")
        void shouldAcceptBookingWhenApproved() {
            // Given
            when(userRepository.findByUsername("caregiver1")).thenReturn(Optional.of(caregiverUser));
            when(caregiverRepository.findByUser(caregiverUser)).thenReturn(Optional.of(caregiver));
            when(bookingRepository.findById(savedBooking.getId())).thenReturn(Optional.of(savedBooking));
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            // When
            BookingResponse response = bookingService.decideBooking("caregiver1", savedBooking.getId(), true);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(BookingStatus.ACCEPTED);
            verify(bookingRepository).save(argThat(booking ->
                    booking.getStatus() == BookingStatus.ACCEPTED
            ));
        }

        @Test
        @DisplayName("Should reject booking when caregiver declines")
        void shouldRejectBookingWhenDeclined() {
            // Given
            when(userRepository.findByUsername("caregiver1")).thenReturn(Optional.of(caregiverUser));
            when(caregiverRepository.findByUser(caregiverUser)).thenReturn(Optional.of(caregiver));
            when(bookingRepository.findById(savedBooking.getId())).thenReturn(Optional.of(savedBooking));
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            // When
            BookingResponse response = bookingService.decideBooking("caregiver1", savedBooking.getId(), false);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(BookingStatus.REJECTED);
            verify(bookingRepository).save(argThat(booking ->
                    booking.getStatus() == BookingStatus.REJECTED
            ));
        }

        @Test
        @DisplayName("Should throw exception when caregiver user not found")
        void shouldThrowExceptionWhenCaregiverUserNotFound() {
            // Given
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> bookingService.decideBooking("nonexistent", savedBooking.getId(), true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");

            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when caregiver profile not found")
        void shouldThrowExceptionWhenCaregiverProfileNotFound() {
            // Given
            when(userRepository.findByUsername("caregiver1")).thenReturn(Optional.of(caregiverUser));
            when(caregiverRepository.findByUser(caregiverUser)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> bookingService.decideBooking("caregiver1", savedBooking.getId(), true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Caregiver profile not found");

            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when booking not found")
        void shouldThrowExceptionWhenBookingNotFound() {
            // Given
            when(userRepository.findByUsername("caregiver1")).thenReturn(Optional.of(caregiverUser));
            when(caregiverRepository.findByUser(caregiverUser)).thenReturn(Optional.of(caregiver));
            when(bookingRepository.findById(savedBooking.getId())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> bookingService.decideBooking("caregiver1", savedBooking.getId(), true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Booking not found");

            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when caregiver tries to modify another caregiver's booking")
        void shouldThrowExceptionWhenModifyingOtherCaregiverBooking() {
            // Given
            Caregiver otherCaregiver = new Caregiver();
            otherCaregiver.setId(UUID.randomUUID());
            savedBooking.setCaregiver(otherCaregiver);

            when(userRepository.findByUsername("caregiver1")).thenReturn(Optional.of(caregiverUser));
            when(caregiverRepository.findByUser(caregiverUser)).thenReturn(Optional.of(caregiver));
            when(bookingRepository.findById(savedBooking.getId())).thenReturn(Optional.of(savedBooking));

            // When & Then
            assertThatThrownBy(() -> bookingService.decideBooking("caregiver1", savedBooking.getId(), true))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("You cannot modify this booking");

            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when booking is already processed")
        void shouldThrowExceptionWhenBookingAlreadyProcessed() {
            // Given
            savedBooking.setStatus(BookingStatus.ACCEPTED);
            when(userRepository.findByUsername("caregiver1")).thenReturn(Optional.of(caregiverUser));
            when(caregiverRepository.findByUser(caregiverUser)).thenReturn(Optional.of(caregiver));
            when(bookingRepository.findById(savedBooking.getId())).thenReturn(Optional.of(savedBooking));

            // When & Then
            assertThatThrownBy(() -> bookingService.decideBooking("caregiver1", savedBooking.getId(), true))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Booking already processed");

            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when trying to modify rejected booking")
        void shouldThrowExceptionWhenModifyingRejectedBooking() {
            // Given
            savedBooking.setStatus(BookingStatus.REJECTED);
            when(userRepository.findByUsername("caregiver1")).thenReturn(Optional.of(caregiverUser));
            when(caregiverRepository.findByUser(caregiverUser)).thenReturn(Optional.of(caregiver));
            when(bookingRepository.findById(savedBooking.getId())).thenReturn(Optional.of(savedBooking));

            // When & Then
            assertThatThrownBy(() -> bookingService.decideBooking("caregiver1", savedBooking.getId(), true))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Booking already processed");

            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when trying to modify paid booking")
        void shouldThrowExceptionWhenModifyingPaidBooking() {
            // Given
            savedBooking.setStatus(BookingStatus.PAID);
            when(userRepository.findByUsername("caregiver1")).thenReturn(Optional.of(caregiverUser));
            when(caregiverRepository.findByUser(caregiverUser)).thenReturn(Optional.of(caregiver));
            when(bookingRepository.findById(savedBooking.getId())).thenReturn(Optional.of(savedBooking));

            // When & Then
            assertThatThrownBy(() -> bookingService.decideBooking("caregiver1", savedBooking.getId(), true))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Booking already processed");
        }
    }
}
