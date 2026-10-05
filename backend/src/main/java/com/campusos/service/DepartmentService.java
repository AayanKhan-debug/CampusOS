package com.campusos.service;

import com.campusos.dto.request.CreateDepartmentRequest;
import com.campusos.dto.request.UpdateDepartmentRequest;
import com.campusos.dto.response.DepartmentResponse;
import com.campusos.dto.response.PageResponse;
import com.campusos.entity.Department;
import com.campusos.exception.DuplicateResourceException;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * DepartmentService — CRUD operations for academic departments.
 * ADMIN-only for write operations; all authenticated users can read.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        String trimmedName = request.getName().trim();
        String trimmedCode = request.getCode().trim().toUpperCase();

        if (departmentRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new DuplicateResourceException("A department with name '" + trimmedName + "' already exists");
        }
        if (departmentRepository.existsByCodeIgnoreCase(trimmedCode)) {
            throw new DuplicateResourceException("A department with code '" + trimmedCode + "' already exists");
        }

        Department dept = Department.builder()
                .name(trimmedName)
                .code(trimmedCode)
                .description(request.getDescription())
                .build();

        Department saved = departmentRepository.save(dept);
        log.info("Department '{}' created with code '{}'", saved.getName(), saved.getCode());
        return DepartmentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> getAllDepartments(Pageable pageable) {
        Page<Department> page = departmentRepository.findAll(pageable);
        List<DepartmentResponse> content = page.getContent().stream()
                .map(DepartmentResponse::fromEntity).toList();
        return PageResponse.of(page, content);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        return DepartmentResponse.fromEntity(
                departmentRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id))
        );
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest request) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        if (request.getName() != null && !request.getName().isBlank()) {
            String trimmedName = request.getName().trim();
            if (departmentRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
                throw new DuplicateResourceException("A department with name '" + trimmedName + "' already exists");
            }
            dept.setName(trimmedName);
        }
        if (request.getCode() != null && !request.getCode().isBlank()) {
            String trimmedCode = request.getCode().trim().toUpperCase();
            if (departmentRepository.existsByCodeIgnoreCaseAndIdNot(trimmedCode, id)) {
                throw new DuplicateResourceException("A department with code '" + trimmedCode + "' already exists");
            }
            dept.setCode(trimmedCode);
        }
        if (request.getDescription() != null) {
            dept.setDescription(request.getDescription());
        }

        return DepartmentResponse.fromEntity(departmentRepository.save(dept));
    }

    @Transactional
    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department", "id", id);
        }
        departmentRepository.deleteById(id);
        log.info("Department {} deleted", id);
    }
}
