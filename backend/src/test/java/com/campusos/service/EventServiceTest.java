package com.campusos.service;

import com.campusos.dto.request.CreateEventRequest;
import com.campusos.dto.request.UpdateEventRequest;
import com.campusos.dto.response.EventResponse;
import com.campusos.entity.Club;
import com.campusos.entity.Event;
import com.campusos.entity.EventRegistration;
import com.campusos.entity.User;
import com.campusos.enums.*;
import com.campusos.exception.BadRequestException;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.exception.UnauthorizedException;
import com.campusos.repository.ClubRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Tests")
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository registrationRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventService eventService;

    private User adminUser;
    private User clubAdminUser;
    private User studentUser;
    private Club testClub;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L).name("Admin User").email("admin@campus.edu").role(Role.ADMIN).build();

        clubAdminUser = User.builder()
                .id(2L).name("Club Leader").email("leader@campus.edu").role(Role.CLUB_ADMIN).build();

        studentUser = User.builder()
                .id(3L).name("Student").email("student@campus.edu").role(Role.STUDENT).build();

        testClub = Club.builder()
                .id(10L).name("Tech Club").createdBy(clubAdminUser).status(ClubStatus.ACTIVE).build();

        testEvent = Event.builder()
                .id(100L)
                .title("Spring Boot Workshop")
                .description("Learn Spring Boot fundamentals")
                .category(EventCategory.WORKSHOP)
                .date(LocalDate.now().plusDays(7))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .location("Auditorium A")
                .capacity(50)
                .status(EventStatus.UPCOMING)
                .club(testClub)
                .createdBy(clubAdminUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void mockSecurityContext(String email) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // ── Create Event Tests ──────────────────────────────────────────

    @Test
    @DisplayName("Create Event: CLUB_ADMIN for own club → success")
    void createEvent_clubAdminOwnClub_success() {
        mockSecurityContext(clubAdminUser.getEmail());
        when(userRepository.findByEmail(clubAdminUser.getEmail())).thenReturn(Optional.of(clubAdminUser));
        when(clubRepository.findById(10L)).thenReturn(Optional.of(testClub));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        CreateEventRequest request = CreateEventRequest.builder()
                .title("Spring Boot Workshop")
                .description("Learn Spring Boot fundamentals")
                .category(EventCategory.WORKSHOP)
                .date(LocalDate.now().plusDays(7))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .location("Auditorium A")
                .capacity(50)
                .clubId(10L)
                .build();

        EventResponse response = eventService.createEvent(request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Spring Boot Workshop");
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("Create Event: ADMIN without club → success")
    void createEvent_adminNoClub_success() {
        mockSecurityContext(adminUser.getEmail());
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));

        Event savedEvent = Event.builder()
                .id(101L).title("Campus Fest").category(EventCategory.CULTURAL)
                .date(LocalDate.now().plusDays(14)).startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0)).location("Main Ground")
                .status(EventStatus.UPCOMING).createdBy(adminUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        CreateEventRequest request = CreateEventRequest.builder()
                .title("Campus Fest")
                .category(EventCategory.CULTURAL)
                .date(LocalDate.now().plusDays(14))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .location("Main Ground")
                .build();

        EventResponse response = eventService.createEvent(request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Campus Fest");
        assertThat(response.getClubId()).isNull();
    }

    @Test
    @DisplayName("Create Event: STUDENT → throws UnauthorizedException")
    void createEvent_student_throwsUnauthorized() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));

        CreateEventRequest request = CreateEventRequest.builder()
                .title("My Event").category(EventCategory.OTHER)
                .date(LocalDate.now().plusDays(1)).startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0)).location("Room 101").build();

        assertThatThrownBy(() -> eventService.createEvent(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Students");

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create Event: endTime before startTime → throws BadRequestException")
    void createEvent_invalidTime_throwsBadRequest() {
        mockSecurityContext(adminUser.getEmail());
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));

        CreateEventRequest request = CreateEventRequest.builder()
                .title("Bad Event").category(EventCategory.OTHER)
                .date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(10, 0))
                .location("Room 101").build();

        assertThatThrownBy(() -> eventService.createEvent(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("End time");
    }

    @Test
    @DisplayName("Create Event: CLUB_ADMIN for another's club → throws UnauthorizedException")
    void createEvent_clubAdminOtherClub_throwsUnauthorized() {
        User otherClubAdmin = User.builder()
                .id(5L).name("Other Admin").email("other@campus.edu").role(Role.CLUB_ADMIN).build();

        mockSecurityContext(otherClubAdmin.getEmail());
        when(userRepository.findByEmail(otherClubAdmin.getEmail())).thenReturn(Optional.of(otherClubAdmin));
        when(clubRepository.findById(10L)).thenReturn(Optional.of(testClub)); // testClub is created by clubAdminUser

        CreateEventRequest request = CreateEventRequest.builder()
                .title("Unauthorized Event").category(EventCategory.TECHNICAL)
                .date(LocalDate.now().plusDays(1)).startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0)).location("Room 101").clubId(10L).build();

        assertThatThrownBy(() -> eventService.createEvent(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("clubs you manage");
    }

    @Test
    @DisplayName("Create Event: inactive club → throws BadRequestException")
    void createEvent_inactiveClub_throwsBadRequest() {
        testClub.setStatus(ClubStatus.INACTIVE);
        mockSecurityContext(clubAdminUser.getEmail());
        when(userRepository.findByEmail(clubAdminUser.getEmail())).thenReturn(Optional.of(clubAdminUser));
        when(clubRepository.findById(10L)).thenReturn(Optional.of(testClub));

        CreateEventRequest request = CreateEventRequest.builder()
                .title("Event").category(EventCategory.CLUB)
                .date(LocalDate.now().plusDays(1)).startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0)).location("Room").clubId(10L).build();

        assertThatThrownBy(() -> eventService.createEvent(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inactive");
    }

    // ── Get Event Tests ────────────────────────────────────────────

    @Test
    @DisplayName("Get Event by ID: success → returns event with registration counts")
    void getEventById_success() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(testEvent));
        when(registrationRepository.countByEventIdAndStatus(100L, RegistrationStatus.CONFIRMED)).thenReturn(30L);
        when(registrationRepository.countByEventIdAndStatus(100L, RegistrationStatus.WAITLISTED)).thenReturn(5L);

        EventResponse response = eventService.getEventById(100L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getConfirmedCount()).isEqualTo(30L);
        assertThat(response.getWaitlistedCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Get Event by ID: nonexistent → throws ResourceNotFoundException")
    void getEventById_notFound() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── Update Event Tests ─────────────────────────────────────────

    @Test
    @DisplayName("Update Event: authorized creator → updates successfully")
    void updateEvent_byCreator_success() {
        mockSecurityContext(clubAdminUser.getEmail());
        when(userRepository.findByEmail(clubAdminUser.getEmail())).thenReturn(Optional.of(clubAdminUser));
        when(eventRepository.findById(100L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
        when(registrationRepository.countByEventIdAndStatus(100L, RegistrationStatus.CONFIRMED)).thenReturn(0L);
        when(registrationRepository.countByEventIdAndStatus(100L, RegistrationStatus.WAITLISTED)).thenReturn(0L);

        UpdateEventRequest request = UpdateEventRequest.builder()
                .title("Updated Workshop Title")
                .build();

        EventResponse response = eventService.updateEvent(100L, request);

        assertThat(response).isNotNull();
        verify(eventRepository).save(testEvent);
    }

    @Test
    @DisplayName("Update Event: unauthorized user → throws UnauthorizedException")
    void updateEvent_byStudent_throwsUnauthorized() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
        when(eventRepository.findById(100L)).thenReturn(Optional.of(testEvent));

        UpdateEventRequest request = UpdateEventRequest.builder()
                .title("Hacked Title").build();

        assertThatThrownBy(() -> eventService.updateEvent(100L, request))
                .isInstanceOf(UnauthorizedException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update Event: cancelled event → throws BadRequestException")
    void updateEvent_cancelledEvent_throwsBadRequest() {
        testEvent.setStatus(EventStatus.CANCELLED);
        mockSecurityContext(clubAdminUser.getEmail());
        when(userRepository.findByEmail(clubAdminUser.getEmail())).thenReturn(Optional.of(clubAdminUser));
        when(eventRepository.findById(100L)).thenReturn(Optional.of(testEvent));

        UpdateEventRequest request = UpdateEventRequest.builder()
                .title("Updated Title").build();

        assertThatThrownBy(() -> eventService.updateEvent(100L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cancelled");
    }

    // ── Cancel Event Tests ─────────────────────────────────────────

    @Test
    @DisplayName("Cancel Event: authorized → soft cancels and cancels all registrations")
    void cancelEvent_success() {
        mockSecurityContext(clubAdminUser.getEmail());
        when(userRepository.findByEmail(clubAdminUser.getEmail())).thenReturn(Optional.of(clubAdminUser));
        when(eventRepository.findById(100L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        List<EventRegistration> confirmedRegs = List.of(
                EventRegistration.builder().id(1L).event(testEvent).user(studentUser)
                        .status(RegistrationStatus.CONFIRMED).registeredAt(LocalDateTime.now()).build()
        );
        when(registrationRepository.findByEventIdAndStatus(100L, RegistrationStatus.CONFIRMED)).thenReturn(confirmedRegs);
        when(registrationRepository.findByEventIdAndStatus(100L, RegistrationStatus.WAITLISTED)).thenReturn(List.of());
        when(registrationRepository.saveAll(anyList())).thenReturn(confirmedRegs);

        eventService.cancelEvent(100L);

        assertThat(testEvent.getStatus()).isEqualTo(EventStatus.CANCELLED);
        verify(eventRepository).save(testEvent);
        verify(registrationRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Cancel Event: already cancelled → throws BadRequestException")
    void cancelEvent_alreadyCancelled_throwsBadRequest() {
        testEvent.setStatus(EventStatus.CANCELLED);
        mockSecurityContext(clubAdminUser.getEmail());
        when(userRepository.findByEmail(clubAdminUser.getEmail())).thenReturn(Optional.of(clubAdminUser));
        when(eventRepository.findById(100L)).thenReturn(Optional.of(testEvent));

        assertThatThrownBy(() -> eventService.cancelEvent(100L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already cancelled");
    }
}
