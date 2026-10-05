package com.campusos.service;

import com.campusos.dto.request.CreateSubjectRequest;
import com.campusos.dto.request.UpdateSubjectRequest;
import com.campusos.dto.response.PageResponse;
import com.campusos.dto.response.SubjectResponse;
import com.campusos.entity.Course;
import com.campusos.entity.Subject;
import com.campusos.exception.DuplicateResourceException;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.repository.CourseRepository;
import com.campusos.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SubjectService — CRUD for academic subjects within courses.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public SubjectResponse createSubject(CreateSubjectRequest request) {
        String trimmedCode = request.getCode().trim().toUpperCase();

        if (subjectRepository.existsByCodeIgnoreCase(trimmedCode)) {
            throw new DuplicateResourceException("A subject with code '" + trimmedCode + "' already exists");
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));

        Subject subject = Subject.builder()
                .name(request.getName().trim())
                .code(trimmedCode)
                .semester(request.getSemester())
                .course(course)
                .description(request.getDescription())
                .build();

        Subject saved = subjectRepository.save(subject);
        log.info("Subject '{}' created in course '{}'", saved.getName(), course.getName());
        return SubjectResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<SubjectResponse> getAllSubjects(Long courseId, Integer semester, Pageable pageable) {
        Page<Subject> page;
        if (courseId != null && semester != null) {
            page = subjectRepository.findByCourseIdAndSemester(courseId, semester, pageable);
        } else if (courseId != null) {
            page = subjectRepository.findByCourseId(courseId, pageable);
        } else {
            page = subjectRepository.findAll(pageable);
        }

        List<SubjectResponse> content = page.getContent().stream()
                .map(SubjectResponse::fromEntity).toList();
        return PageResponse.of(page, content);
    }

    @Transactional(readOnly = true)
    public SubjectResponse getSubjectById(Long id) {
        return SubjectResponse.fromEntity(
                subjectRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", id))
        );
    }

    @Transactional
    public SubjectResponse updateSubject(Long id, UpdateSubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", id));

        if (request.getName() != null && !request.getName().isBlank()) {
            subject.setName(request.getName().trim());
        }
        if (request.getCode() != null && !request.getCode().isBlank()) {
            String trimmedCode = request.getCode().trim().toUpperCase();
            if (subjectRepository.existsByCodeIgnoreCaseAndIdNot(trimmedCode, id)) {
                throw new DuplicateResourceException("A subject with code '" + trimmedCode + "' already exists");
            }
            subject.setCode(trimmedCode);
        }
        if (request.getSemester() != null) {
            subject.setSemester(request.getSemester());
        }
        if (request.getCourseId() != null) {
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));
            subject.setCourse(course);
        }
        if (request.getDescription() != null) {
            subject.setDescription(request.getDescription());
        }

        return SubjectResponse.fromEntity(subjectRepository.save(subject));
    }

    @Transactional
    public void deleteSubject(Long id) {
        if (!subjectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subject", "id", id);
        }
        subjectRepository.deleteById(id);
        log.info("Subject {} deleted", id);
    }
}
