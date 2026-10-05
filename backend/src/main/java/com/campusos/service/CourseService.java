package com.campusos.service;

import com.campusos.dto.request.CreateCourseRequest;
import com.campusos.dto.request.UpdateCourseRequest;
import com.campusos.dto.response.CourseResponse;
import com.campusos.dto.response.PageResponse;
import com.campusos.entity.Course;
import com.campusos.entity.Department;
import com.campusos.exception.DuplicateResourceException;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.repository.CourseRepository;
import com.campusos.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CourseService — CRUD for academic courses within departments.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request) {
        String trimmedCode = request.getCode().trim().toUpperCase();

        if (courseRepository.existsByCodeIgnoreCase(trimmedCode)) {
            throw new DuplicateResourceException("A course with code '" + trimmedCode + "' already exists");
        }

        Department dept = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));

        Course course = Course.builder()
                .name(request.getName().trim())
                .code(trimmedCode)
                .department(dept)
                .description(request.getDescription())
                .build();

        Course saved = courseRepository.save(course);
        log.info("Course '{}' created in department '{}'", saved.getName(), dept.getName());
        return CourseResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getAllCourses(Long departmentId, Pageable pageable) {
        Page<Course> page = departmentId != null
                ? courseRepository.findByDepartmentId(departmentId, pageable)
                : courseRepository.findAll(pageable);

        List<CourseResponse> content = page.getContent().stream()
                .map(CourseResponse::fromEntity).toList();
        return PageResponse.of(page, content);
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long id) {
        return CourseResponse.fromEntity(
                courseRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id))
        );
    }

    @Transactional
    public CourseResponse updateCourse(Long id, UpdateCourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));

        if (request.getName() != null && !request.getName().isBlank()) {
            course.setName(request.getName().trim());
        }
        if (request.getCode() != null && !request.getCode().isBlank()) {
            String trimmedCode = request.getCode().trim().toUpperCase();
            if (courseRepository.existsByCodeIgnoreCaseAndIdNot(trimmedCode, id)) {
                throw new DuplicateResourceException("A course with code '" + trimmedCode + "' already exists");
            }
            course.setCode(trimmedCode);
        }
        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
            course.setDepartment(dept);
        }
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }

        return CourseResponse.fromEntity(courseRepository.save(course));
    }

    @Transactional
    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Course", "id", id);
        }
        courseRepository.deleteById(id);
        log.info("Course {} deleted", id);
    }
}
