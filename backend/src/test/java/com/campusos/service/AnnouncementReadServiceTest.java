package com.campusos.service;

import com.campusos.dto.response.AnnouncementReadResponse;
import com.campusos.entity.Announcement;
import com.campusos.entity.AnnouncementRead;
import com.campusos.entity.User;
import com.campusos.enums.AnnouncementCategory;
import com.campusos.enums.AnnouncementStatus;
import com.campusos.enums.Role;
import com.campusos.exception.BadRequestException;
import com.campusos.exception.DuplicateResourceException;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.repository.AnnouncementReadRepository;
import com.campusos.repository.AnnouncementRepository;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnnouncementReadService Tests")
class AnnouncementReadServiceTest {

    @Mock private AnnouncementReadRepository readRepository;
    @Mock private AnnouncementRepository announcementRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private AnnouncementReadService readService;

    private User studentUser;
    private User anotherStudent;
    private Announcement activeAnnouncement;
    private Announcement archivedAnnouncement;

    @BeforeEach
    void setUp() {
        studentUser = User.builder().id(1L).name("Student A").email("studentA@campus.edu").role(Role.STUDENT).build();
        anotherStudent = User.builder().id(2L).name("Student B").email("studentB@campus.edu").role(Role.STUDENT).build();

        activeAnnouncement = Announcement.builder()
                .id(100L).title("Active Notice").content("This is an active announcement for all students.")
                .category(AnnouncementCategory.GENERAL).isPinned(false)
                .publishedAt(LocalDateTime.now().minusHours(1))
                .status(AnnouncementStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusHours(1)).updatedAt(LocalDateTime.now().minusHours(1))
                .build();

        archivedAnnouncement = Announcement.builder()
                .id(200L).title("Old Notice").content("This announcement has been archived already.")
                .category(AnnouncementCategory.GENERAL).isPinned(false)
                .publishedAt(LocalDateTime.now().minusDays(10))
                .status(AnnouncementStatus.ARCHIVED)
                .createdAt(LocalDateTime.now().minusDays(10)).updatedAt(LocalDateTime.now().minusDays(5))
                .build();
    }

    private void mockSecurityContext(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    @DisplayName("Mark as read: first time → success, returns read response")
    void markAsRead_firstTime_success() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
        when(announcementRepository.findById(100L)).thenReturn(Optional.of(activeAnnouncement));
        when(readRepository.existsByAnnouncementIdAndUserId(100L, studentUser.getId())).thenReturn(false);

        AnnouncementRead saved = AnnouncementRead.builder()
                .id(1L).announcement(activeAnnouncement).user(studentUser)
                .readAt(LocalDateTime.now()).createdAt(LocalDateTime.now()).build();
        when(readRepository.save(any(AnnouncementRead.class))).thenReturn(saved);

        AnnouncementReadResponse response = readService.markAsRead(100L);

        assertThat(response).isNotNull();
        assertThat(response.getAnnouncementId()).isEqualTo(100L);
        assertThat(response.getAnnouncementTitle()).isEqualTo("Active Notice");
        verify(readRepository).save(any(AnnouncementRead.class));
    }

    @Test
    @DisplayName("Mark as read: duplicate → throws DuplicateResourceException")
    void markAsRead_duplicate_throwsDuplicate() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
        when(announcementRepository.findById(100L)).thenReturn(Optional.of(activeAnnouncement));
        when(readRepository.existsByAnnouncementIdAndUserId(100L, studentUser.getId())).thenReturn(true);

        assertThatThrownBy(() -> readService.markAsRead(100L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already marked");

        verify(readRepository, never()).save(any());
    }

    @Test
    @DisplayName("Mark as read: archived announcement → throws BadRequestException")
    void markAsRead_archived_throwsBadRequest() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
        when(announcementRepository.findById(200L)).thenReturn(Optional.of(archivedAnnouncement));

        assertThatThrownBy(() -> readService.markAsRead(200L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("active");

        verify(readRepository, never()).save(any());
    }

    @Test
    @DisplayName("Mark as read: nonexistent announcement → throws ResourceNotFoundException")
    void markAsRead_notFound_throws() {
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
        when(announcementRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> readService.markAsRead(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("User isolation: different users can both mark the same announcement as read")
    void markAsRead_userIsolation_bothCanRead() {
        // StudentA marks as read
        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
        when(announcementRepository.findById(100L)).thenReturn(Optional.of(activeAnnouncement));
        when(readRepository.existsByAnnouncementIdAndUserId(100L, studentUser.getId())).thenReturn(false);

        AnnouncementRead savedA = AnnouncementRead.builder()
                .id(1L).announcement(activeAnnouncement).user(studentUser)
                .readAt(LocalDateTime.now()).createdAt(LocalDateTime.now()).build();
        when(readRepository.save(any(AnnouncementRead.class))).thenReturn(savedA);

        AnnouncementReadResponse responseA = readService.markAsRead(100L);
        assertThat(responseA.getAnnouncementId()).isEqualTo(100L);

        // StudentB also marks as read (different user, no duplicate)
        mockSecurityContext(anotherStudent.getEmail());
        when(userRepository.findByEmail(anotherStudent.getEmail())).thenReturn(Optional.of(anotherStudent));
        when(announcementRepository.findById(100L)).thenReturn(Optional.of(activeAnnouncement));
        when(readRepository.existsByAnnouncementIdAndUserId(100L, anotherStudent.getId())).thenReturn(false);

        AnnouncementRead savedB = AnnouncementRead.builder()
                .id(2L).announcement(activeAnnouncement).user(anotherStudent)
                .readAt(LocalDateTime.now()).createdAt(LocalDateTime.now()).build();
        when(readRepository.save(any(AnnouncementRead.class))).thenReturn(savedB);

        AnnouncementReadResponse responseB = readService.markAsRead(100L);
        assertThat(responseB.getAnnouncementId()).isEqualTo(100L);

        verify(readRepository, times(2)).save(any(AnnouncementRead.class));
    }
}
