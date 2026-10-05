package com.campusos.repository;

import com.campusos.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * CourseRepository — data access for Course entities.
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Page<Course> findByDepartmentId(Long departmentId, Pageable pageable);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
