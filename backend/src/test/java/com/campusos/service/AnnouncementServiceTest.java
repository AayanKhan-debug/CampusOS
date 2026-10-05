package com.campusos.service;

import com.campusos.dto.request.CreateAnnouncementRequest;
import com.campusos.dto.request.UpdateAnnouncementRequest;
import com.campusos.dto.response.AnnouncementResponse;
import com.campusos.dto.response.PageResponse;
import com.campusos.entity.Announcement;
import com.campusos.entity.User;
import com.campusos.enums.AnnouncementCategory;
import com.campusos.enums.AnnouncementStatus;
import com.campusos.enums.Role;
import com.campusos.exception.BadRequestException;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.exception.UnauthorizedException;
import com.campusos.repository.AnnouncementRepository;
import com.campusos.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnnouncementService Tests")
class AnnouncementServiceTest {

    @Mock private AnnouncementRepository announcementRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private AnnouncementService announcementService;

    private User adminUser;
    private User clubAdminUser;
    private User studentUser;
    private Announcement testAnnouncement;

    @BeforeEach
    void setUp() {
        adminUser = User.builder().id(1L).name("Admin").email("admin@campus.edu").role(Role.ADMIN).build();
        clubAdminUser = User.builder().id(2L).name("Leader").email("leader@campus.edu").role(Role.CLUB_ADMIN).build();
        studentUser = User.builder().id(3L).name("Student").email("student@campus.edu").role(Role.STUDENT).build();

        testAnnouncement = Announcement.builder()
                .id(10L)
                .title("Semester Exams Rescheduled")
                .content("Due to unforeseen circumstances, semester exams are rescheduled to next week.")
                .category(AnnouncementCategory.EXAM)
                .isPinned(false)
                .publishedAt(LocalDateTime.now().minusHours(1))
                .status(AnnouncementStatus.ACTIVE)
                .createdBy(adminUser)
                .createdAt(LocalDateTime.now().minusHours(1))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    private void mockSecurityContext(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    // ── Create Tests ──────────────────────────────────────────────

    @Test
    @DisplayName("Create: ADMIN creates announcement → success")
    void createAnnouncement_admin_success() {
        mockSecurityContext(adminUser.getEmail());
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));
        when(announcementRepository.save(any(Announcement.class))).thenReturn(testAnnouncement);

        CreateAnnouncementRequest request = CreateAnnouncementRequest.builder()
                .title("Semester Exams Rescheduled")
                .content("Due to unforeseen circumstances, semester exams are rescheduled to next week.")
                .category(AnnouncementCategory.EXAM)
                .build();

        AnnouncementResponse response = announcementService.createAnnouncement(request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Semester Exams Rescheduled");
        verify(announcementRepository).save(any(Announcement.class));
    }

    @Test
    @DisplayName("Create: CLUB_ADMIN creates announcement → success")
    void createAnnouncement_clubAdmin_success() {
        mockSecurityContext(clubAdminUser.getEmail());
        when(userRepository.findByEmail(clubAdminUser.getEmail())).thenReturn(Optional.of(clubAdminUser));

        Announcement clubAnn = Announcement.builder()
                .id(11L).title("Club Meeting").content("Monthly club meeting this Friday at 4 PM in Room 101.")
                .category(AnnouncementCategory.CLUB).isPinned(false)
                .publishedAt(LocalDateTime.now()).status(AnnouncementStatus.ACTIVE)
                .createdBy(clubAdminUser).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        when(announcementRepository.save(any(Announcement.class))).thenReturn(clubAnn);

        CreateAnnouncementRequest request = CreateAnnouncementRequest.builder()
                .title("Club Meeting")
                .content("Monthly club meeting this Friday at 4 PM in Room 101.")
                .category(AnnouncementCategory.CLUB)
                .build();

        AnnouncementResponse response = announcementService.createAnnouncement(request);

        assertThat(response.getTitle()).isEqualTo("Club Meeting");
    }

    @Test
    @DisplayName("Create: STUDENT blocked → throws UnauthorizedException")
    void createAnnouncement_student_throwsUnauthorized() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));

        CreateAnnouncementRequest request = CreateAnnouncementRequest.builder()
                .title("Hack Title")
                .content("Some content that is long enough.")
                .category(AnnouncementCategory.GENERAL)
                .build();

        assertThatThrownBy(() -> announcementService.createAnnouncement(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Students");

        verify(announcementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create: expiresAt before publishedAt → throws BadRequestException")
    void createAnnouncement_invalidDates_throwsBadRequest() {
        mockSecurityContext(adminUser.getEmail());
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));

        CreateAnnouncementRequest request = CreateAnnouncementRequest.builder()
                .title("Test Announcement")
                .content("Content that is long enough to pass validation here.")
                .category(AnnouncementCategory.GENERAL)
                .publishedAt(LocalDateTime.now().plusDays(2))
                .expiresAt(LocalDateTime.now().plusDays(1))  // before publishedAt
                .build();

        assertThatThrownBy(() -> announcementService.createAnnouncement(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Expiry date");
    }

    // ── Get Tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("Get by ID: existing → returns announcement")
    void getAnnouncementById_success() {
        when(announcementRepository.findById(10L)).thenReturn(Optional.of(testAnnouncement));

        AnnouncementResponse response = announcementService.getAnnouncementById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getCategory()).isEqualTo(AnnouncementCategory.EXAM);
    }

    @Test
    @DisplayName("Get by ID: nonexistent → throws ResourceNotFoundException")
    void getAnnouncementById_notFound() {
        when(announcementRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> announcementService.getAnnouncementById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── Update Tests ──────────────────────────────────────────────

    @Test
    @DisplayName("Update: owner updates title → success")
    void updateAnnouncement_owner_success() {
        mockSecurityContext(adminUser.getEmail());
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));
        when(announcementRepository.findById(10L)).thenReturn(Optional.of(testAnnouncement));
        when(announcementRepository.save(any(Announcement.class))).thenReturn(testAnnouncement);

        UpdateAnnouncementRequest request = UpdateAnnouncementRequest.builder()
                .title("Updated Exam Notification")
                .build();

        AnnouncementResponse response = announcementService.updateAnnouncement(10L, request);

        assertThat(response).isNotNull();
        verify(announcementRepository).save(testAnnouncement);
    }

    @Test
    @DisplayName("Update: non-owner → throws UnauthorizedException")
    void updateAnnouncement_nonOwner_throwsUnauthorized() {
        mockSecurityContext(clubAdminUser.getEmail());
        when(userRepository.findByEmail(clubAdminUser.getEmail())).thenReturn(Optional.of(clubAdminUser));
        when(announcementRepository.findById(10L)).thenReturn(Optional.of(testAnnouncement)); // created by admin

        assertThatThrownBy(() -> announcementService.updateAnnouncement(10L,
                UpdateAnnouncementRequest.builder().title("Hacked").build()))
                .isInstanceOf(UnauthorizedException.class);

        verify(announcementRepository, never()).save(any());
    }

    // ── Delete Tests ──────────────────────────────────────────────

    @Test
    @DisplayName("Delete: ADMIN deletes any announcement → success")
    void deleteAnnouncement_admin_success() {
        mockSecurityContext(adminUser.getEmail());
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));
        when(announcementRepository.findById(10L)).thenReturn(Optional.of(testAnnouncement));

        announcementService.deleteAnnouncement(10L);

        verify(announcementRepository).delete(testAnnouncement);
    }

    // ── Pin Tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("Toggle pin: unpinned → becomes pinned")
    void togglePin_success() {
        mockSecurityContext(adminUser.getEmail());
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));
        when(announcementRepository.findById(10L)).thenReturn(Optional.of(testAnnouncement));

        Announcement pinned = Announcement.builder()
                .id(10L).title(testAnnouncement.getTitle()).content(testAnnouncement.getContent())
                .category(testAnnouncement.getCategory()).isPinned(true)
                .publishedAt(testAnnouncement.getPublishedAt()).status(AnnouncementStatus.ACTIVE)
                .createdBy(adminUser).createdAt(testAnnouncement.getCreatedAt()).updatedAt(LocalDateTime.now())
                .build();
        when(announcementRepository.save(any(Announcement.class))).thenReturn(pinned);

        AnnouncementResponse response = announcementService.togglePin(10L);

        assertThat(response.isPinned()).isTrue();
    }

    // ── Archive Tests ─────────────────────────────────────────────

    @Test
    @DisplayName("Archive: ACTIVE → ARCHIVED")
    void archiveAnnouncement_success() {
        mockSecurityContext(adminUser.getEmail());
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));
        when(announcementRepository.findById(10L)).thenReturn(Optional.of(testAnnouncement));

        Announcement archived = Announcement.builder()
                .id(10L).title(testAnnouncement.getTitle()).content(testAnnouncement.getContent())
                .category(testAnnouncement.getCategory()).isPinned(false)
                .publishedAt(testAnnouncement.getPublishedAt()).status(AnnouncementStatus.ARCHIVED)
                .createdBy(adminUser).createdAt(testAnnouncement.getCreatedAt()).updatedAt(LocalDateTime.now())
                .build();
        when(announcementRepository.save(any(Announcement.class))).thenReturn(archived);

        AnnouncementResponse response = announcementService.archiveAnnouncement(10L);

        assertThat(response.getStatus()).isEqualTo(AnnouncementStatus.ARCHIVED);
    }

    @Test
    @DisplayName("Archive: already archived → throws BadRequestException")
    void archiveAnnouncement_alreadyArchived_throws() {
        testAnnouncement.setStatus(AnnouncementStatus.ARCHIVED);
        mockSecurityContext(adminUser.getEmail());
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));
        when(announcementRepository.findById(10L)).thenReturn(Optional.of(testAnnouncement));

        assertThatThrownBy(() -> announcementService.archiveAnnouncement(10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already archived");
    }

    // ── Auto-Expire Tests ─────────────────────────────────────────

    @Test
    @DisplayName("Auto-expire: past expiresAt → sets EXPIRED status")
    void autoExpire_setsExpiredStatus() {
        Announcement expirable = Announcement.builder()
                .id(20L).title("Old Notice").content("This announcement should expire now.")
                .category(AnnouncementCategory.GENERAL).isPinned(false)
                .publishedAt(LocalDateTime.now().minusDays(5))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .status(AnnouncementStatus.ACTIVE)
                .createdBy(adminUser).createdAt(LocalDateTime.now().minusDays(5)).updatedAt(LocalDateTime.now())
                .build();

        when(announcementRepository.findExpiredActiveAnnouncements(any(LocalDateTime.class)))
                .thenReturn(List.of(expirable));
        when(announcementRepository.saveAll(anyList())).thenReturn(List.of(expirable));

        announcementService.autoExpireAnnouncements();

        assertThat(expirable.getStatus()).isEqualTo(AnnouncementStatus.EXPIRED);
        verify(announcementRepository).saveAll(anyList());
    }
}
