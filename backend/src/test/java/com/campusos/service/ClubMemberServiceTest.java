package com.campusos.service;

import com.campusos.dto.request.MembershipStatusUpdateRequest;
import com.campusos.dto.response.ClubMemberResponse;
import com.campusos.entity.Club;
import com.campusos.entity.ClubMember;
import com.campusos.entity.User;
import com.campusos.enums.ClubStatus;
import com.campusos.enums.MembershipStatus;
import com.campusos.enums.Role;
import com.campusos.exception.BadRequestException;
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
@DisplayName("ClubMemberService Tests")
class ClubMemberServiceTest {

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClubMemberService clubMemberService;

    private User clubLeader;
    private User student;
    private User student2;
    private Club testClub;
    private Club inactiveClub;

    @BeforeEach
    void setUp() {
        clubLeader = User.builder()
                .id(1L)
                .name("Club Leader")
                .email("leader@campus.edu")
                .role(Role.CLUB_ADMIN)
                .build();

        student = User.builder()
                .id(2L)
                .name("Alice Student")
                .email("alice@campus.edu")
                .role(Role.STUDENT)
                .build();

        student2 = User.builder()
                .id(3L)
                .name("Bob Student")
                .email("bob@campus.edu")
                .role(Role.STUDENT)
                .build();

        testClub = Club.builder()
                .id(10L)
                .name("Robotics Club")
                .status(ClubStatus.ACTIVE)
                .createdBy(clubLeader)
                .build();

        inactiveClub = Club.builder()
                .id(11L)
                .name("Old Inactive Club")
                .status(ClubStatus.INACTIVE)
                .createdBy(clubLeader)
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
    @DisplayName("Join Club: student joins active club → creates PENDING membership")
    void joinClub_success_createsPending() {
        mockSecurityContext(student.getEmail());
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(clubRepository.findById(10L)).thenReturn(Optional.of(testClub));
        when(clubMemberRepository.findByClubIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        ClubMember savedPending = ClubMember.builder()
                .id(100L)
                .club(testClub)
                .user(student)
                .status(MembershipStatus.PENDING)
                .build();
        when(clubMemberRepository.save(any(ClubMember.class))).thenReturn(savedPending);

        ClubMemberResponse response = clubMemberService.joinClub(10L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(MembershipStatus.PENDING);
        verify(clubMemberRepository).save(any(ClubMember.class));
    }

    @Test
    @DisplayName("Join Club: already APPROVED member → throws DuplicateResourceException")
    void joinClub_alreadyApproved_throwsException() {
        mockSecurityContext(student.getEmail());
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(clubRepository.findById(10L)).thenReturn(Optional.of(testClub));

        ClubMember existingApproved = ClubMember.builder()
                .id(100L)
                .club(testClub)
                .user(student)
                .status(MembershipStatus.APPROVED)
                .build();
        when(clubMemberRepository.findByClubIdAndUserId(10L, 2L)).thenReturn(Optional.of(existingApproved));

        assertThatThrownBy(() -> clubMemberService.joinClub(10L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already an approved member");

        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("Join Club: already PENDING request → throws DuplicateResourceException")
    void joinClub_alreadyPending_throwsException() {
        mockSecurityContext(student.getEmail());
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(clubRepository.findById(10L)).thenReturn(Optional.of(testClub));

        ClubMember existingPending = ClubMember.builder()
                .id(100L)
                .club(testClub)
                .user(student)
                .status(MembershipStatus.PENDING)
                .build();
        when(clubMemberRepository.findByClubIdAndUserId(10L, 2L)).thenReturn(Optional.of(existingPending));

        assertThatThrownBy(() -> clubMemberService.joinClub(10L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already have a pending membership request");

        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("Join Club: previously REJECTED request → resets to PENDING")
    void joinClub_previouslyRejected_resetsToPending() {
        mockSecurityContext(student.getEmail());
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(clubRepository.findById(10L)).thenReturn(Optional.of(testClub));

        ClubMember existingRejected = ClubMember.builder()
                .id(100L)
                .club(testClub)
                .user(student)
                .status(MembershipStatus.REJECTED)
                .build();
        when(clubMemberRepository.findByClubIdAndUserId(10L, 2L)).thenReturn(Optional.of(existingRejected));
        when(clubMemberRepository.save(existingRejected)).thenReturn(existingRejected);

        ClubMemberResponse response = clubMemberService.joinClub(10L);

        assertThat(response).isNotNull();
        assertThat(existingRejected.getStatus()).isEqualTo(MembershipStatus.PENDING);
        verify(clubMemberRepository).save(existingRejected);
    }

    @Test
    @DisplayName("Join Club: deactivated club → throws BadRequestException")
    void joinClub_inactiveClub_throwsBadRequest() {
        mockSecurityContext(student.getEmail());
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(clubRepository.findById(11L)).thenReturn(Optional.of(inactiveClub));

        assertThatThrownBy(() -> clubMemberService.joinClub(11L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inactive or deactivated");

        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("Join Club: nonexistent club → throws ResourceNotFoundException")
    void joinClub_nonexistentClub_throwsNotFound() {
        mockSecurityContext(student.getEmail());
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(clubRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubMemberService.joinClub(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Approve Membership: authorized club leader approves request → status APPROVED with joinedAt")
    void updateMemberStatus_approve_success() {
        mockSecurityContext(clubLeader.getEmail());
        when(userRepository.findByEmail(clubLeader.getEmail())).thenReturn(Optional.of(clubLeader));
        when(clubRepository.findById(10L)).thenReturn(Optional.of(testClub));

        ClubMember pendingMember = ClubMember.builder()
                .id(100L)
                .club(testClub)
                .user(student)
                .status(MembershipStatus.PENDING)
                .build();
        when(clubMemberRepository.findByClubIdAndUserId(10L, 2L)).thenReturn(Optional.of(pendingMember));
        when(clubMemberRepository.save(any(ClubMember.class))).thenReturn(pendingMember);

        MembershipStatusUpdateRequest updateReq = MembershipStatusUpdateRequest.builder()
                .status(MembershipStatus.APPROVED)
                .build();

        ClubMemberResponse response = clubMemberService.updateMemberStatus(10L, 2L, updateReq);

        assertThat(response).isNotNull();
        assertThat(pendingMember.getStatus()).isEqualTo(MembershipStatus.APPROVED);
        assertThat(pendingMember.getJoinedAt()).isNotNull();
        verify(clubMemberRepository).save(pendingMember);
    }

    @Test
    @DisplayName("Reject Membership: authorized club leader rejects request → status REJECTED")
    void updateMemberStatus_reject_success() {
        mockSecurityContext(clubLeader.getEmail());
        when(userRepository.findByEmail(clubLeader.getEmail())).thenReturn(Optional.of(clubLeader));
        when(clubRepository.findById(10L)).thenReturn(Optional.of(testClub));

        ClubMember pendingMember = ClubMember.builder()
                .id(100L)
                .club(testClub)
                .user(student)
                .status(MembershipStatus.PENDING)
                .build();
        when(clubMemberRepository.findByClubIdAndUserId(10L, 2L)).thenReturn(Optional.of(pendingMember));
        when(clubMemberRepository.save(any(ClubMember.class))).thenReturn(pendingMember);

        MembershipStatusUpdateRequest updateReq = MembershipStatusUpdateRequest.builder()
                .status(MembershipStatus.REJECTED)
                .build();

        ClubMemberResponse response = clubMemberService.updateMemberStatus(10L, 2L, updateReq);

        assertThat(response).isNotNull();
        assertThat(pendingMember.getStatus()).isEqualTo(MembershipStatus.REJECTED);
        verify(clubMemberRepository).save(pendingMember);
    }

    @Test
    @DisplayName("Update Membership: unauthorized user → throws UnauthorizedException")
    void updateMemberStatus_unauthorized_throwsException() {
        mockSecurityContext(student2.getEmail());
        when(userRepository.findByEmail(student2.getEmail())).thenReturn(Optional.of(student2));
        when(clubRepository.findById(10L)).thenReturn(Optional.of(testClub));

        MembershipStatusUpdateRequest updateReq = MembershipStatusUpdateRequest.builder()
                .status(MembershipStatus.APPROVED)
                .build();

        assertThatThrownBy(() -> clubMemberService.updateMemberStatus(10L, 2L, updateReq))
                .isInstanceOf(UnauthorizedException.class);

        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update Membership: self approval → throws BadRequestException")
    void updateMemberStatus_selfApproval_throwsBadRequest() {
        mockSecurityContext(clubLeader.getEmail());
        when(userRepository.findByEmail(clubLeader.getEmail())).thenReturn(Optional.of(clubLeader));
        when(clubRepository.findById(10L)).thenReturn(Optional.of(testClub));

        MembershipStatusUpdateRequest updateReq = MembershipStatusUpdateRequest.builder()
                .status(MembershipStatus.APPROVED)
                .build();

        // clubLeader ID is 1L, trying to approve target userId 1L
        assertThatThrownBy(() -> clubMemberService.updateMemberStatus(10L, 1L, updateReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot approve or reject your own");
    }

    @Test
    @DisplayName("Update Membership: invalid transition from already APPROVED → throws BadRequestException")
    void updateMemberStatus_invalidTransition_throwsBadRequest() {
        mockSecurityContext(clubLeader.getEmail());
        when(userRepository.findByEmail(clubLeader.getEmail())).thenReturn(Optional.of(clubLeader));
        when(clubRepository.findById(10L)).thenReturn(Optional.of(testClub));

        ClubMember alreadyApproved = ClubMember.builder()
                .id(100L)
                .club(testClub)
                .user(student)
                .status(MembershipStatus.APPROVED)
                .build();
        when(clubMemberRepository.findByClubIdAndUserId(10L, 2L)).thenReturn(Optional.of(alreadyApproved));

        MembershipStatusUpdateRequest updateReq = MembershipStatusUpdateRequest.builder()
                .status(MembershipStatus.REJECTED)
                .build();

        assertThatThrownBy(() -> clubMemberService.updateMemberStatus(10L, 2L, updateReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only PENDING requests can be APPROVED or REJECTED");
    }
}
