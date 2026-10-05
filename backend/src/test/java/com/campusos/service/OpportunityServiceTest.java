package com.campusos.service;

import com.campusos.dto.request.CreateOpportunityRequest;
import com.campusos.dto.request.UpdateOpportunityRequest;
import com.campusos.dto.response.OpportunityResponse;
import com.campusos.dto.response.PageResponse;
import com.campusos.entity.Opportunity;
import com.campusos.entity.User;
import com.campusos.enums.OpportunityStatus;
import com.campusos.enums.OpportunityType;
import com.campusos.enums.Role;
import com.campusos.exception.BadRequestException;
import com.campusos.exception.UnauthorizedException;
import com.campusos.repository.OpportunityRepository;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpportunityService Tests")
class OpportunityServiceTest {

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OpportunityService opportunityService;

    private User student;
    private User clubLeader1;
    private User clubLeader2;
    private User admin;
    private Opportunity testOpp;

    @BeforeEach
    void setUp() {
        student = User.builder().id(1L).email("student@campus.edu").name("Student").role(Role.STUDENT).build();
        clubLeader1 = User.builder().id(2L).email("leader1@campus.edu").name("Leader 1").role(Role.CLUB_ADMIN).build();
        clubLeader2 = User.builder().id(3L).email("leader2@campus.edu").name("Leader 2").role(Role.CLUB_ADMIN).build();
        admin = User.builder().id(4L).email("admin@campus.edu").name("Admin").role(Role.ADMIN).build();

        testOpp = Opportunity.builder()
                .id(100L)
                .title("Software Engineering Intern")
                .description("Summer internship program")
                .organization("Google")
                .opportunityType(OpportunityType.INTERNSHIP)
                .applicationUrl("https://careers.google.com")
                .deadline(LocalDateTime.now().plusDays(30))
                .status(OpportunityStatus.OPEN)
                .remote(true)
                .createdBy(clubLeader1)
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
    @DisplayName("createOpportunity — CLUB_ADMIN can create successfully")
    void testCreateOpportunity_ClubAdminSuccess() {
        mockAuth(clubLeader1);

        CreateOpportunityRequest req = CreateOpportunityRequest.builder()
                .title("Hackathon 2026")
                .description("Annual 24-hour hackathon")
                .organization("Tech Club")
                .opportunityType(OpportunityType.HACKATHON)
                .applicationUrl("https://hackathon.campus.edu")
                .deadline(LocalDateTime.now().plusDays(10))
                .remote(false)
                .build();

        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(i -> {
            Opportunity opp = i.getArgument(0);
            opp.setId(101L);
            return opp;
        });

        OpportunityResponse resp = opportunityService.createOpportunity(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getTitle()).isEqualTo("Hackathon 2026");
        assertThat(resp.getStatus()).isEqualTo(OpportunityStatus.OPEN);
        assertThat(resp.getCreatedBy().getEmail()).isEqualTo("leader1@campus.edu");
    }

    @Test
    @DisplayName("createOpportunity — STUDENT cannot create (403 Unauthorized)")
    void testCreateOpportunity_Student_ThrowsUnauthorized() {
        mockAuth(student);

        CreateOpportunityRequest req = CreateOpportunityRequest.builder()
                .title("Internship")
                .description("Desc")
                .organization("Org")
                .opportunityType(OpportunityType.INTERNSHIP)
                .build();

        assertThatThrownBy(() -> opportunityService.createOpportunity(req))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("createOpportunity — rejects past deadline")
    void testCreateOpportunity_PastDeadline_ThrowsBadRequest() {
        mockAuth(clubLeader1);

        CreateOpportunityRequest req = CreateOpportunityRequest.builder()
                .title("Internship")
                .description("Desc")
                .organization("Org")
                .opportunityType(OpportunityType.INTERNSHIP)
                .deadline(LocalDateTime.now().minusDays(1))
                .build();

        assertThatThrownBy(() -> opportunityService.createOpportunity(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("past");
    }

    @Test
    @DisplayName("createOpportunity — rejects invalid applicationUrl without http/https")
    void testCreateOpportunity_InvalidUrl_ThrowsBadRequest() {
        mockAuth(clubLeader1);

        CreateOpportunityRequest req = CreateOpportunityRequest.builder()
                .title("Internship")
                .description("Desc")
                .organization("Org")
                .opportunityType(OpportunityType.INTERNSHIP)
                .applicationUrl("ftp://example.com/apply")
                .build();

        assertThatThrownBy(() -> opportunityService.createOpportunity(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("http:// or https://");
    }

    @Test
    @DisplayName("updateOpportunity — creator can update successfully")
    void testUpdateOpportunity_CreatorSuccess() {
        mockAuth(clubLeader1);
        when(opportunityRepository.findById(100L)).thenReturn(Optional.of(testOpp));
        when(opportunityRepository.save(any(Opportunity.class))).thenReturn(testOpp);

        UpdateOpportunityRequest req = UpdateOpportunityRequest.builder()
                .title("Updated Title")
                .build();

        OpportunityResponse resp = opportunityService.updateOpportunity(100L, req);
        assertThat(resp.getTitle()).isEqualTo("Updated Title");
    }

    @Test
    @DisplayName("updateOpportunity — non-creator CLUB_ADMIN cannot update (403)")
    void testUpdateOpportunity_OtherLeader_ThrowsUnauthorized() {
        mockAuth(clubLeader2);
        when(opportunityRepository.findById(100L)).thenReturn(Optional.of(testOpp));

        UpdateOpportunityRequest req = UpdateOpportunityRequest.builder()
                .title("Hijacked Title")
                .build();

        assertThatThrownBy(() -> opportunityService.updateOpportunity(100L, req))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("deleteOpportunity — ADMIN can delete any opportunity")
    void testDeleteOpportunity_AdminSuccess() {
        mockAuth(admin);
        when(opportunityRepository.findById(100L)).thenReturn(Optional.of(testOpp));

        opportunityService.deleteOpportunity(100L);
        verify(opportunityRepository, times(1)).delete(testOpp);
    }

    @Test
    @DisplayName("getAllOpportunities — returns active open opportunities")
    void testGetAllOpportunities_Success() {
        Page<Opportunity> page = new PageImpl<>(List.of(testOpp));
        when(opportunityRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<OpportunityResponse> resp = opportunityService.getAllOpportunities(
                OpportunityType.INTERNSHIP, "Google", true, PageRequest.of(0, 10));

        assertThat(resp.getContent()).hasSize(1);
        assertThat(resp.getContent().get(0).getOrganization()).isEqualTo("Google");
    }
}
