package com.campusos.service;

import com.campusos.dto.request.CreateClubRequest;
import com.campusos.dto.request.UpdateClubRequest;
import com.campusos.dto.response.ClubResponse;
import com.campusos.entity.Club;
import com.campusos.entity.ClubMember;
import com.campusos.entity.User;
import com.campusos.enums.ClubStatus;
import com.campusos.enums.Role;
import com.campusos.exception.DuplicateResourceException;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.exception.UnauthorizedException;
import com.campusos.repository.ClubMemberRepository;
import com.campusos.repository.ClubRepository;
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
@DisplayName("ClubService Tests")
class ClubServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClubService clubService;

    private User adminUser;
    private User clubAdminUser;
    private User otherUser;
    private Club testClub;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .name("Admin User")
                .email("admin@campus.edu")
                .role(Role.ADMIN)
                .build();

        clubAdminUser = User.builder()
                .id(2L)
                .name("Club Leader")
                .email("leader@campus.edu")
                .role(Role.CLUB_ADMIN)
                .build();

        otherUser = User.builder()
                .id(3L)
                .name("Student User")
                .email("student@campus.edu")
                .role(Role.STUDENT)
                .build();

        testClub = Club.builder()
                .id(10L)
                .name("Coding Club")
                .description("All things software")
                .category("Technical")
                .logo("https://example.com/logo.png")
                .createdBy(clubAdminUser)
                .status(ClubStatus.ACTIVE)
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

    @Test
    @DisplayName("Create Club: success → creates club and registers creator as approved member")
    void createClub_success() {
        mockSecurityContext(clubAdminUser.getEmail());
        when(userRepository.findByEmail(clubAdminUser.getEmail())).thenReturn(Optional.of(clubAdminUser));
        when(clubRepository.existsByNameIgnoreCase("Coding Club")).thenReturn(false);
        when(clubRepository.save(any(Club.class))).thenReturn(testClub);
        when(clubMemberRepository.save(any(ClubMember.class))).thenReturn(new ClubMember());

        CreateClubRequest request = CreateClubRequest.builder()
                .name("Coding Club")
                .description("All things software")
                .category("Technical")
                .logo("https://example.com/logo.png")
                .build();

        ClubResponse response = clubService.createClub(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Coding Club");
        assertThat(response.getStatus()).isEqualTo(ClubStatus.ACTIVE);
        verify(clubRepository).save(any(Club.class));
        verify(clubMemberRepository).save(any(ClubMember.class));
    }

    @Test
    @DisplayName("Create Club: duplicate name → throws DuplicateResourceException")
    void createClub_duplicateName_throwsException() {
        mockSecurityContext(clubAdminUser.getEmail());
        when(userRepository.findByEmail(clubAdminUser.getEmail())).thenReturn(Optional.of(clubAdminUser));
        when(clubRepository.existsByNameIgnoreCase("Coding Club")).thenReturn(true);

        CreateClubRequest request = CreateClubRequest.builder()
                .name("Coding Club")
                .build();

        assertThatThrownBy(() -> clubService.createClub(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Coding Club");

        verify(clubRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get Club by ID: success → returns club response with member count")
    void getClubById_success() {
        when(clubRepository.findById(10L)).thenReturn(Optional.of(testClub));
        when(clubMemberRepository.countByClubIdAndStatus(10L, com.campusos.enums.MembershipStatus.APPROVED)).thenReturn(15L);

        ClubResponse response = clubService.getClubById(10L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getMemberCount()).isEqualTo(15L);
    }

    @Test
    @DisplayName("Get Club by ID: nonexistent → throws ResourceNotFoundException")
    void getClubById_notFound() {
        when(clubRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubService.getClubById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Update Club: authorized creator → updates successfully")
    void updateClub_byCreator_success() {
        mockSecurityContext(clubAdminUser.getEmail());
        when(userRepository.findByEmail(clubAdminUser.getEmail())).thenReturn(Optional.of(clubAdminUser));
        when(clubRepository.findById(10L)).thenReturn(Optional.of(testClub));
        when(clubRepository.save(any(Club.class))).thenReturn(testClub);
        when(clubMemberRepository.countByClubIdAndStatus(10L, com.campusos.enums.MembershipStatus.APPROVED)).thenReturn(1L);

        UpdateClubRequest request = UpdateClubRequest.builder()
                .description("Updated description")
                .build();

        ClubResponse response = clubService.updateClub(10L, request);

        assertThat(response).isNotNull();
        verify(clubRepository).save(testClub);
    }

    @Test
    @DisplayName("Update Club: unauthorized user → throws UnauthorizedException")
    void updateClub_byUnauthorizedUser_throwsException() {
        mockSecurityContext(otherUser.getEmail());
        when(userRepository.findByEmail(otherUser.getEmail())).thenReturn(Optional.of(otherUser));
        when(clubRepository.findById(10L)).thenReturn(Optional.of(testClub));

        UpdateClubRequest request = UpdateClubRequest.builder()
                .description("Hacked description")
                .build();

        assertThatThrownBy(() -> clubService.updateClub(10L, request))
                .isInstanceOf(UnauthorizedException.class);

        verify(clubRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete Club: soft-deactivates club status to INACTIVE")
    void deleteClub_softDeactivates() {
        mockSecurityContext(clubAdminUser.getEmail());
        when(userRepository.findByEmail(clubAdminUser.getEmail())).thenReturn(Optional.of(clubAdminUser));
        when(clubRepository.findById(10L)).thenReturn(Optional.of(testClub));
        when(clubRepository.save(any(Club.class))).thenReturn(testClub);

        clubService.deleteClub(10L);

        assertThat(testClub.getStatus()).isEqualTo(ClubStatus.INACTIVE);
        verify(clubRepository).save(testClub);
    }
}
