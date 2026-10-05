package com.campusos.service;

import com.campusos.dto.request.*;
import com.campusos.dto.response.HelpDeskIssueResponse;
import com.campusos.dto.response.PageResponse;
import com.campusos.entity.HelpDeskIssue;
import com.campusos.entity.User;
import com.campusos.enums.IssueCategory;
import com.campusos.enums.IssuePriority;
import com.campusos.enums.IssueStatus;
import com.campusos.enums.Role;
import com.campusos.exception.BadRequestException;
import com.campusos.exception.UnauthorizedException;
import com.campusos.repository.HelpDeskRepository;
import com.campusos.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HelpDeskService Tests")
class HelpDeskServiceTest {

    @Mock
    private HelpDeskRepository helpDeskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private HelpDeskService helpDeskService;

    private User student;
    private User staff;
    private User admin;
    private HelpDeskIssue testIssue;

    @BeforeEach
    void setUp() {
        student = User.builder().id(1L).email("student@campus.edu").name("Student").role(Role.STUDENT).build();
        staff = User.builder().id(2L).email("staff@campus.edu").name("Staff Member").role(Role.ADMIN).build();
        admin = User.builder().id(3L).email("admin@campus.edu").name("Admin").role(Role.ADMIN).build();

        testIssue = HelpDeskIssue.builder()
                .id(200L)
                .title("Wi-Fi down in Hostel A")
                .description("No internet connection since morning")
                .category(IssueCategory.INTERNET)
                .location("Hostel A Block 2")
                .priority(IssuePriority.HIGH)
                .status(IssueStatus.SUBMITTED)
                .reportedBy(student)
                .build();
    }

    private void mockAuth(User user) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(user.getEmail());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("createIssue — successfully creates issue in SUBMITTED state")
    void testCreateIssue_Success() {
        mockAuth(student);

        CreateHelpDeskIssueRequest req = CreateHelpDeskIssueRequest.builder()
                .title("Broken switchboard")
                .description("Sparking seen on wall switchboard")
                .category(IssueCategory.ELECTRICAL)
                .location("Lab 302")
                .priority(IssuePriority.URGENT)
                .build();

        when(helpDeskRepository.save(any(HelpDeskIssue.class))).thenAnswer(i -> {
            HelpDeskIssue issue = i.getArgument(0);
            issue.setId(201L);
            return issue;
        });

        HelpDeskIssueResponse resp = helpDeskService.createIssue(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getTitle()).isEqualTo("Broken switchboard");
        assertThat(resp.getStatus()).isEqualTo(IssueStatus.SUBMITTED);
        assertThat(resp.getPriority()).isEqualTo(IssuePriority.URGENT);
        assertThat(resp.getReportedBy().getEmail()).isEqualTo("student@campus.edu");
    }

    @Test
    @DisplayName("updateIssue — reporter can update when status is SUBMITTED")
    void testUpdateIssue_Submitted_Success() {
        mockAuth(student);
        when(helpDeskRepository.findById(200L)).thenReturn(Optional.of(testIssue));
        when(helpDeskRepository.save(any(HelpDeskIssue.class))).thenReturn(testIssue);

        UpdateHelpDeskIssueRequest req = UpdateHelpDeskIssueRequest.builder()
                .title("Wi-Fi down in Hostel A (Fixed Room #)")
                .build();

        HelpDeskIssueResponse resp = helpDeskService.updateIssue(200L, req);
        assertThat(resp.getTitle()).isEqualTo("Wi-Fi down in Hostel A (Fixed Room #)");
    }

    @Test
    @DisplayName("updateIssue — reporter cannot update once IN_PROGRESS (Throws BadRequest)")
    void testUpdateIssue_InProgress_ThrowsBadRequest() {
        testIssue.setStatus(IssueStatus.IN_PROGRESS);
        mockAuth(student);
        when(helpDeskRepository.findById(200L)).thenReturn(Optional.of(testIssue));

        UpdateHelpDeskIssueRequest req = UpdateHelpDeskIssueRequest.builder()
                .title("Updated Title")
                .build();

        assertThatThrownBy(() -> helpDeskService.updateIssue(200L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot update issue once it is IN_PROGRESS");
    }

    @Test
    @DisplayName("deleteIssue — reporter cannot delete once RESOLVED")
    void testDeleteIssue_Resolved_ThrowsBadRequest() {
        testIssue.setStatus(IssueStatus.RESOLVED);
        mockAuth(student);
        when(helpDeskRepository.findById(200L)).thenReturn(Optional.of(testIssue));

        assertThatThrownBy(() -> helpDeskService.deleteIssue(200L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete issue once it is RESOLVED");
    }

    @Test
    @DisplayName("assignIssue — ADMIN assigns staff and moves status to IN_PROGRESS")
    void testAssignIssue_AdminSuccess() {
        mockAuth(admin);
        when(helpDeskRepository.findById(200L)).thenReturn(Optional.of(testIssue));
        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));
        when(helpDeskRepository.save(any(HelpDeskIssue.class))).thenAnswer(i -> i.getArgument(0));

        AssignIssueRequest req = new AssignIssueRequest(2L);
        HelpDeskIssueResponse resp = helpDeskService.assignIssue(200L, req);

        assertThat(resp.getAssignedTo()).isNotNull();
        assertThat(resp.getAssignedTo().getEmail()).isEqualTo("staff@campus.edu");
        assertThat(resp.getStatus()).isEqualTo(IssueStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("assignIssue — STUDENT cannot assign (403 Unauthorized)")
    void testAssignIssue_Student_ThrowsUnauthorized() {
        mockAuth(student);

        AssignIssueRequest req = new AssignIssueRequest(2L);
        assertThatThrownBy(() -> helpDeskService.assignIssue(200L, req))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("resolveIssue — ADMIN can resolve with notes")
    void testResolveIssue_AdminSuccess() {
        testIssue.setStatus(IssueStatus.IN_PROGRESS);
        mockAuth(admin);
        when(helpDeskRepository.findById(200L)).thenReturn(Optional.of(testIssue));
        when(helpDeskRepository.save(any(HelpDeskIssue.class))).thenAnswer(i -> i.getArgument(0));

        ResolveIssueRequest req = new ResolveIssueRequest("Replaced damaged fiber cable and restarted router.");
        HelpDeskIssueResponse resp = helpDeskService.resolveIssue(200L, req);

        assertThat(resp.getStatus()).isEqualTo(IssueStatus.RESOLVED);
        assertThat(resp.getResolvedAt()).isNotNull();
        assertThat(resp.getResolutionNotes()).isEqualTo("Replaced damaged fiber cable and restarted router.");
    }

    @Test
    @DisplayName("updateStatus — student can close their own RESOLVED issue")
    void testUpdateStatus_StudentCloseResolved_Success() {
        testIssue.setStatus(IssueStatus.RESOLVED);
        mockAuth(student);
        when(helpDeskRepository.findById(200L)).thenReturn(Optional.of(testIssue));
        when(helpDeskRepository.save(any(HelpDeskIssue.class))).thenAnswer(i -> i.getArgument(0));

        UpdateIssueStatusRequest req = new UpdateIssueStatusRequest(IssueStatus.CLOSED);
        HelpDeskIssueResponse resp = helpDeskService.updateStatus(200L, req);

        assertThat(resp.getStatus()).isEqualTo(IssueStatus.CLOSED);
    }

    @Test
    @DisplayName("updateStatus — student cannot close SUBMITTED issue directly")
    void testUpdateStatus_StudentCloseSubmitted_ThrowsBadRequest() {
        testIssue.setStatus(IssueStatus.SUBMITTED);
        mockAuth(student);
        when(helpDeskRepository.findById(200L)).thenReturn(Optional.of(testIssue));

        UpdateIssueStatusRequest req = new UpdateIssueStatusRequest(IssueStatus.CLOSED);
        assertThatThrownBy(() -> helpDeskService.updateStatus(200L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only close an issue that has been resolved");
    }

    @Test
    @DisplayName("getAllIssues — returns prioritized issues for admin")
    void testGetAllIssues_PrioritySorted() {
        Page<HelpDeskIssue> page = new PageImpl<>(List.of(testIssue));
        when(helpDeskRepository.findWithFiltersAndPrioritySort(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<HelpDeskIssueResponse> resp = helpDeskService.getAllIssues(null, null, null, PageRequest.of(0, 10));

        assertThat(resp.getContent()).hasSize(1);
        assertThat(resp.getContent().get(0).getTitle()).isEqualTo("Wi-Fi down in Hostel A");
    }
}
