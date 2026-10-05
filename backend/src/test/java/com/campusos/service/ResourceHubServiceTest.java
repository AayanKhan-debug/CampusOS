package com.campusos.service;

import com.campusos.dto.request.CreateDepartmentRequest;
import com.campusos.dto.request.CreateCourseRequest;
import com.campusos.dto.request.CreateSubjectRequest;
import com.campusos.dto.request.CreateResourceRequest;
import com.campusos.dto.response.CourseResponse;
import com.campusos.dto.response.DepartmentResponse;
import com.campusos.dto.response.ResourceResponse;
import com.campusos.dto.response.SubjectResponse;
import com.campusos.entity.*;
import com.campusos.enums.ResourceType;
import com.campusos.enums.Role;
import com.campusos.exception.DuplicateResourceException;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.exception.UnauthorizedException;
import com.campusos.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Resource Hub Service Tests")
class ResourceHubServiceTest {

    // ── Mocks for Department ──────────────────────────────────────
    @Mock private DepartmentRepository departmentRepository;
    @InjectMocks private DepartmentService departmentService;

    // ── Mocks for Course ──────────────────────────────────────────
    @Mock private CourseRepository courseRepository;

    // ── Mocks for Subject ─────────────────────────────────────────
    @Mock private SubjectRepository subjectRepository;

    // ── Mocks for Resource ────────────────────────────────────────
    @Mock private ResourceRepository resourceRepository;
    @Mock private UserRepository userRepository;

    private User adminUser;
    private User studentUser;
    private Department testDept;
    private Course testCourse;
    private Subject testSubject;
    private Resource testResource;

    @BeforeEach
    void setUp() {
        adminUser = User.builder().id(1L).name("Admin").email("admin@campus.edu").role(Role.ADMIN).build();
        studentUser = User.builder().id(2L).name("Student").email("student@campus.edu").role(Role.STUDENT).build();

        testDept = Department.builder()
                .id(1L).name("Computer Science & Engineering").code("CSE")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        testCourse = Course.builder()
                .id(1L).name("B.E. Computer Science").code("BE-CS")
                .department(testDept)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        testSubject = Subject.builder()
                .id(1L).name("Data Structures").code("CS-DS-301").semester(3)
                .course(testCourse)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        testResource = Resource.builder()
                .id(1L).title("DS Notes Chapter 1").description("Complete notes for chapter 1.")
                .resourceType(ResourceType.NOTES).url("https://drive.google.com/ds-notes.pdf")
                .subject(testSubject).uploadedBy(adminUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private void mockSecurityContext(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    // ── Department Tests ──────────────────────────────────────────

    @Test
    @DisplayName("Department Create: unique name/code → success")
    void createDepartment_success() {
        when(departmentRepository.existsByNameIgnoreCase("Computer Science & Engineering")).thenReturn(false);
        when(departmentRepository.existsByCodeIgnoreCase("CSE")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(testDept);

        DepartmentResponse response = departmentService.createDepartment(
                CreateDepartmentRequest.builder()
                        .name("Computer Science & Engineering").code("cse").build());

        assertThat(response.getName()).isEqualTo("Computer Science & Engineering");
        assertThat(response.getCode()).isEqualTo("CSE");
    }

    @Test
    @DisplayName("Department Create: duplicate name → throws DuplicateResourceException")
    void createDepartment_duplicateName_throws() {
        when(departmentRepository.existsByNameIgnoreCase("Computer Science & Engineering")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.createDepartment(
                CreateDepartmentRequest.builder().name("Computer Science & Engineering").code("CSE2").build()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Computer Science & Engineering");
    }

    @Test
    @DisplayName("Department Create: duplicate code → throws DuplicateResourceException")
    void createDepartment_duplicateCode_throws() {
        when(departmentRepository.existsByNameIgnoreCase("New Department")).thenReturn(false);
        when(departmentRepository.existsByCodeIgnoreCase("CSE")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.createDepartment(
                CreateDepartmentRequest.builder().name("New Department").code("CSE").build()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Department Get by ID: found → returns response")
    void getDepartmentById_found() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDept));

        DepartmentResponse response = departmentService.getDepartmentById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCode()).isEqualTo("CSE");
    }

    @Test
    @DisplayName("Department Get by ID: not found → throws ResourceNotFoundException")
    void getDepartmentById_notFound() {
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getDepartmentById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Department Delete: found → deletes successfully")
    void deleteDepartment_success() {
        when(departmentRepository.existsById(1L)).thenReturn(true);

        departmentService.deleteDepartment(1L);

        verify(departmentRepository).deleteById(1L);
    }

    // ── Course Tests ──────────────────────────────────────────────

    @Test
    @DisplayName("Course Create: valid department → success")
    void createCourse_success() {
        CourseService courseService = new CourseService(courseRepository, departmentRepository);

        when(courseRepository.existsByCodeIgnoreCase("BE-CS")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDept));
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

        CourseResponse response = courseService.createCourse(
                CreateCourseRequest.builder()
                        .name("B.E. Computer Science").code("be-cs").departmentId(1L).build());

        assertThat(response.getName()).isEqualTo("B.E. Computer Science");
        assertThat(response.getDepartmentName()).isEqualTo("Computer Science & Engineering");
    }

    @Test
    @DisplayName("Course Create: invalid department → throws ResourceNotFoundException")
    void createCourse_invalidDept_throws() {
        CourseService courseService = new CourseService(courseRepository, departmentRepository);

        when(courseRepository.existsByCodeIgnoreCase(anyString())).thenReturn(false);
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.createCourse(
                CreateCourseRequest.builder().name("Test").code("TC-01").departmentId(999L).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Subject Tests ─────────────────────────────────────────────

    @Test
    @DisplayName("Subject Create: valid course → success")
    void createSubject_success() {
        SubjectService subjectService = new SubjectService(subjectRepository, courseRepository);

        when(subjectRepository.existsByCodeIgnoreCase("CS-DS-301")).thenReturn(false);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(subjectRepository.save(any(Subject.class))).thenReturn(testSubject);

        SubjectResponse response = subjectService.createSubject(
                CreateSubjectRequest.builder()
                        .name("Data Structures").code("cs-ds-301").semester(3).courseId(1L).build());

        assertThat(response.getName()).isEqualTo("Data Structures");
        assertThat(response.getSemester()).isEqualTo(3);
        assertThat(response.getCourseName()).isEqualTo("B.E. Computer Science");
    }

    @Test
    @DisplayName("Subject Create: invalid course → throws ResourceNotFoundException")
    void createSubject_invalidCourse_throws() {
        SubjectService subjectService = new SubjectService(subjectRepository, courseRepository);

        when(subjectRepository.existsByCodeIgnoreCase(anyString())).thenReturn(false);
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.createSubject(
                CreateSubjectRequest.builder().name("Test").code("TS-01").semester(1).courseId(999L).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Resource Tests ────────────────────────────────────────────

    @Test
    @DisplayName("Resource Create: ADMIN uploads → success with uploadedBy set")
    void createResource_admin_success() {
        ResourceHubService resourceHubService = new ResourceHubService(resourceRepository, subjectRepository, userRepository);

        mockSecurityContext(adminUser.getEmail());
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(resourceRepository.save(any(Resource.class))).thenReturn(testResource);

        ResourceResponse response = resourceHubService.createResource(
                CreateResourceRequest.builder()
                        .title("DS Notes Chapter 1").resourceType(ResourceType.NOTES)
                        .url("https://drive.google.com/ds-notes.pdf").subjectId(1L).build());

        assertThat(response.getTitle()).isEqualTo("DS Notes Chapter 1");
        assertThat(response.getUploadedBy().getEmail()).isEqualTo("admin@campus.edu");
        assertThat(response.getResourceType()).isEqualTo(ResourceType.NOTES);
    }

    @Test
    @DisplayName("Resource Create: STUDENT → throws UnauthorizedException")
    void createResource_student_throwsUnauthorized() {
        ResourceHubService resourceHubService = new ResourceHubService(resourceRepository, subjectRepository, userRepository);

        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));

        assertThatThrownBy(() -> resourceHubService.createResource(
                CreateResourceRequest.builder()
                        .title("Test").resourceType(ResourceType.NOTES)
                        .url("http://test.com").subjectId(1L).build()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Resource Create: invalid subject → throws ResourceNotFoundException")
    void createResource_invalidSubject_throws() {
        ResourceHubService resourceHubService = new ResourceHubService(resourceRepository, subjectRepository, userRepository);

        mockSecurityContext(adminUser.getEmail());
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));
        when(subjectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceHubService.createResource(
                CreateResourceRequest.builder()
                        .title("Test").resourceType(ResourceType.NOTES)
                        .url("http://test.com").subjectId(999L).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Resource Get by ID: found → returns response with hierarchy")
    void getResourceById_success() {
        ResourceHubService resourceHubService = new ResourceHubService(resourceRepository, subjectRepository, userRepository);

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));

        ResourceResponse response = resourceHubService.getResourceById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSubjectName()).isEqualTo("Data Structures");
        assertThat(response.getCourseName()).isEqualTo("B.E. Computer Science");
        assertThat(response.getDepartmentName()).isEqualTo("Computer Science & Engineering");
    }

    @Test
    @DisplayName("Resource Delete: non-owner → throws UnauthorizedException")
    void deleteResource_nonOwner_throws() {
        ResourceHubService resourceHubService = new ResourceHubService(resourceRepository, subjectRepository, userRepository);

        mockSecurityContext(studentUser.getEmail());
        when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource)); // uploaded by admin

        assertThatThrownBy(() -> resourceHubService.deleteResource(1L))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Resource Delete: ADMIN deletes any → success")
    void deleteResource_admin_success() {
        ResourceHubService resourceHubService = new ResourceHubService(resourceRepository, subjectRepository, userRepository);

        mockSecurityContext(adminUser.getEmail());
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));

        resourceHubService.deleteResource(1L);

        verify(resourceRepository).delete(testResource);
    }
}
