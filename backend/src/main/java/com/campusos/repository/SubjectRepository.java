package com.campusos.repository;

import com.campusos.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * SubjectRepository — data access for Subject entities.
 */
@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Page<Subject> findByCourseId(Long courseId, Pageable pageable);

    Page<Subject> findByCourseIdAndSemester(Long courseId, Integer semester, Pageable pageable);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
