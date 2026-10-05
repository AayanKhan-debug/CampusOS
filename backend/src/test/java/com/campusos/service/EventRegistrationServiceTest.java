package com.campusos.service;

import com.campusos.dto.response.EventRegistrationResponse;
import com.campusos.dto.response.EventRegistrationStatusResponse;
import com.campusos.entity.Event;
import com.campusos.entity.EventRegistration;
import com.campusos.entity.User;
import com.campusos.enums.*;
import com.campusos.exception.BadRequestException;
import com.campusos.exception.DuplicateResourceException;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.repository.EventRegistrationRepository;
import com.campusos.repository.EventRepository;
import com.campusos.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventRegistrationService Tests")
class EventRegistrationServiceTest {

    @Mock
    private EventRegistrationRepository registrationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventRegistrationService registrationService;

    private User studentUser;
    private User anotherStudent;
    private Event upcomingEvent;
    private Event cancelledEvent;
    private Event completedEvent;
    private Event unlimitedEvent;

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .id(1L).name("Student A").email("student@campus.edu").role(Role.STUDENT).build();

        anotherStudent = User.builder()
                .id(2L).name("Student B").email("student2@campus.edu").role(Role.STUDENT).build();

        upcomingEvent = Event.builder()
                .id(100L).title("Workshop").category(EventCategory.WORKSHOP)
                .date(LocalDate.now().plusDays(7))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .location("Room A").capacity(2).status(EventStatus.UPCOMING)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        cancelledEvent = Event.builder()
                .id(200L).title("Cancelled Event").category(EventCategory.OTHER)
                .date(LocalDate.now().plusDays(7))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .location("Room B").status(EventStatus.CANCELLED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        completedEvent = Event.builder()
                .id(300L).title("Past Event").category(EventCategory.SEMINAR)
                .date(LocalDate.now().plusDays(7))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .location("Room C").status(EventStatus.COMPLETED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        unlimitedEvent = Event.builder()
                .id(400L).title("Open Event").category(EventCategory.CULTURAL)
                .date(LocalDate.now().plusDays(7))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .location("Main Hall").capacity(null).status(EventStatus.UPCOMING)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private void mockSecurityContext(String email) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // ── Register Tests ──────────────────────────────────────────────

    @Test
    @DisplayName("Register: capacity available → CONFIRMED")
    void register_capacityAvailable_confirmed() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
        when(eventRepository.findById(100L)).thenReturn(Optional.of(upcomingEvent));
        when(registrationRepository.findActiveRegistration(100L, studentUser.getId())).thenReturn(Optional.empty());
        when(registrationRepository.countConfirmedWithLock(100L)).thenReturn(1L); // 1 of 2 capacity used

        EventRegistration saved = EventRegistration.builder()
                .id(1L).event(upcomingEvent).user(studentUser)
                .status(RegistrationStatus.CONFIRMED).registeredAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(registrationRepository.save(any(EventRegistration.class))).thenReturn(saved);

        EventRegistrationResponse response = registrationService.registerForEvent(100L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Register: capacity full → WAITLISTED")
    void register_capacityFull_waitlisted() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
        when(eventRepository.findById(100L)).thenReturn(Optional.of(upcomingEvent));
        when(registrationRepository.findActiveRegistration(100L, studentUser.getId())).thenReturn(Optional.empty());
        when(registrationRepository.countConfirmedWithLock(100L)).thenReturn(2L); // 2 of 2 capacity = full

        EventRegistration saved = EventRegistration.builder()
                .id(2L).event(upcomingEvent).user(studentUser)
                .status(RegistrationStatus.WAITLISTED).registeredAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(registrationRepository.save(any(EventRegistration.class))).thenReturn(saved);

        EventRegistrationResponse response = registrationService.registerForEvent(100L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(RegistrationStatus.WAITLISTED);
    }

    @Test
    @DisplayName("Register: unlimited capacity → always CONFIRMED")
    void register_unlimitedCapacity_confirmed() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
        when(eventRepository.findById(400L)).thenReturn(Optional.of(unlimitedEvent));
        when(registrationRepository.findActiveRegistration(400L, studentUser.getId())).thenReturn(Optional.empty());

        EventRegistration saved = EventRegistration.builder()
                .id(3L).event(unlimitedEvent).user(studentUser)
                .status(RegistrationStatus.CONFIRMED).registeredAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(registrationRepository.save(any(EventRegistration.class))).thenReturn(saved);

        EventRegistrationResponse response = registrationService.registerForEvent(400L);

        assertThat(response.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
        // Should NOT call countConfirmedWithLock for unlimited events
        verify(registrationRepository, never()).countConfirmedWithLock(anyLong());
    }

    @Test
    @DisplayName("Register: duplicate active registration → throws DuplicateResourceException")
    void register_duplicateActive_throwsDuplicate() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
        when(eventRepository.findById(100L)).thenReturn(Optional.of(upcomingEvent));

        EventRegistration existing = EventRegistration.builder()
                .id(1L).event(upcomingEvent).user(studentUser)
                .status(RegistrationStatus.CONFIRMED).registeredAt(LocalDateTime.now()).build();
        when(registrationRepository.findActiveRegistration(100L, studentUser.getId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> registrationService.registerForEvent(100L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already have an active registration");
    }

    @Test
    @DisplayName("Register: cancelled event → throws BadRequestException")
    void register_cancelledEvent_throwsBadRequest() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
        when(eventRepository.findById(200L)).thenReturn(Optional.of(cancelledEvent));

        assertThatThrownBy(() -> registrationService.registerForEvent(200L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    @DisplayName("Register: completed event → throws BadRequestException")
    void register_completedEvent_throwsBadRequest() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
        when(eventRepository.findById(300L)).thenReturn(Optional.of(completedEvent));

        assertThatThrownBy(() -> registrationService.registerForEvent(300L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("completed");
    }

    @Test
    @DisplayName("Register: past registration deadline → throws BadRequestException")
    void register_deadlinePassed_throwsBadRequest() {
        upcomingEvent.setRegistrationDeadline(LocalDateTime.now().minusDays(1));
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
        when(eventRepository.findById(100L)).thenReturn(Optional.of(upcomingEvent));

        assertThatThrownBy(() -> registrationService.registerForEvent(100L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("deadline");
    }

    @Test
    @DisplayName("Register: nonexistent event → throws ResourceNotFoundException")
    void register_nonexistentEvent_throwsNotFound() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.registerForEvent(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── Cancel Registration Tests ──────────────────────────────────

    @Test
    @DisplayName("Cancel: CONFIRMED user → cancels and promotes waitlisted")
    void cancel_confirmedUser_promotesWaitlisted() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));

        EventRegistration confirmedReg = EventRegistration.builder()
                .id(1L).event(upcomingEvent).user(studentUser)
                .status(RegistrationStatus.CONFIRMED).registeredAt(LocalDateTime.now()).build();
        when(registrationRepository.findByEventIdAndUserId(100L, studentUser.getId())).thenReturn(Optional.of(confirmedReg));
        when(registrationRepository.save(confirmedReg)).thenReturn(confirmedReg);

        // Waitlisted user to promote
        EventRegistration waitlistedReg = EventRegistration.builder()
                .id(2L).event(upcomingEvent).user(anotherStudent)
                .status(RegistrationStatus.WAITLISTED).registeredAt(LocalDateTime.now().minusHours(1)).build();
        when(registrationRepository.findWaitlistedByEventOrderByRegisteredAt(100L)).thenReturn(List.of(waitlistedReg));
        when(registrationRepository.save(waitlistedReg)).thenReturn(waitlistedReg);

        EventRegistrationStatusResponse response = registrationService.cancelRegistration(100L);

        assertThat(response.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
        assertThat(confirmedReg.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
        assertThat(confirmedReg.getCancelledAt()).isNotNull();
        assertThat(waitlistedReg.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
        verify(registrationRepository, times(2)).save(any(EventRegistration.class));
    }

    @Test
    @DisplayName("Cancel: WAITLISTED user → cancels without promoting")
    void cancel_waitlistedUser_noPromotion() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));

        EventRegistration waitlistedReg = EventRegistration.builder()
                .id(1L).event(upcomingEvent).user(studentUser)
                .status(RegistrationStatus.WAITLISTED).registeredAt(LocalDateTime.now()).build();
        when(registrationRepository.findByEventIdAndUserId(100L, studentUser.getId())).thenReturn(Optional.of(waitlistedReg));
        when(registrationRepository.save(waitlistedReg)).thenReturn(waitlistedReg);

        EventRegistrationStatusResponse response = registrationService.cancelRegistration(100L);

        assertThat(response.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
        assertThat(waitlistedReg.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
        // Should NOT call findWaitlistedByEventOrderByRegisteredAt
        verify(registrationRepository, never()).findWaitlistedByEventOrderByRegisteredAt(anyLong());
    }

    @Test
    @DisplayName("Cancel: already cancelled → throws BadRequestException")
    void cancel_alreadyCancelled_throwsBadRequest() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));

        EventRegistration cancelledReg = EventRegistration.builder()
                .id(1L).event(upcomingEvent).user(studentUser)
                .status(RegistrationStatus.CANCELLED).registeredAt(LocalDateTime.now())
                .cancelledAt(LocalDateTime.now()).build();
        when(registrationRepository.findByEventIdAndUserId(100L, studentUser.getId())).thenReturn(Optional.of(cancelledReg));

        assertThatThrownBy(() -> registrationService.cancelRegistration(100L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    @DisplayName("Cancel: no registration found → throws ResourceNotFoundException")
    void cancel_noRegistration_throwsNotFound() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
        when(registrationRepository.findByEventIdAndUserId(100L, studentUser.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.cancelRegistration(100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Registration not found");
    }

    @Test
    @DisplayName("Cancel: CONFIRMED but no waitlisted users → cancels without error")
    void cancel_confirmedNoWaitlist_success() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));

        EventRegistration confirmedReg = EventRegistration.builder()
                .id(1L).event(upcomingEvent).user(studentUser)
                .status(RegistrationStatus.CONFIRMED).registeredAt(LocalDateTime.now()).build();
        when(registrationRepository.findByEventIdAndUserId(100L, studentUser.getId())).thenReturn(Optional.of(confirmedReg));
        when(registrationRepository.save(confirmedReg)).thenReturn(confirmedReg);

        // No waitlisted users
        when(registrationRepository.findWaitlistedByEventOrderByRegisteredAt(100L)).thenReturn(List.of());

        EventRegistrationStatusResponse response = registrationService.cancelRegistration(100L);

        assertThat(response.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
        // Only one save (the cancellation), no promotion save
        verify(registrationRepository, times(1)).save(any(EventRegistration.class));
    }

    // ── Waitlist Promotion Order Test ──────────────────────────────

    @Test
    @DisplayName("Waitlist promotion: promotes earliest WAITLISTED by registeredAt")
    void cancel_promotesEarliestWaitlisted() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));

        EventRegistration confirmedReg = EventRegistration.builder()
                .id(1L).event(upcomingEvent).user(studentUser)
                .status(RegistrationStatus.CONFIRMED).registeredAt(LocalDateTime.now()).build();
        when(registrationRepository.findByEventIdAndUserId(100L, studentUser.getId())).thenReturn(Optional.of(confirmedReg));
        when(registrationRepository.save(confirmedReg)).thenReturn(confirmedReg);

        User thirdStudent = User.builder()
                .id(3L).name("Student C").email("student3@campus.edu").role(Role.STUDENT).build();

        // Two waitlisted users — anotherStudent registered first
        EventRegistration waitlisted1 = EventRegistration.builder()
                .id(2L).event(upcomingEvent).user(anotherStudent)
                .status(RegistrationStatus.WAITLISTED)
                .registeredAt(LocalDateTime.now().minusHours(2)).build();
        EventRegistration waitlisted2 = EventRegistration.builder()
                .id(3L).event(upcomingEvent).user(thirdStudent)
                .status(RegistrationStatus.WAITLISTED)
                .registeredAt(LocalDateTime.now().minusHours(1)).build();

        when(registrationRepository.findWaitlistedByEventOrderByRegisteredAt(100L))
                .thenReturn(List.of(waitlisted1, waitlisted2));
        when(registrationRepository.save(waitlisted1)).thenReturn(waitlisted1);

        registrationService.cancelRegistration(100L);

        // Only the earliest (waitlisted1) should be promoted
        assertThat(waitlisted1.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
        assertThat(waitlisted2.getStatus()).isEqualTo(RegistrationStatus.WAITLISTED);
    }
}
