package com.campusos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Subject entity — third level of the Resource Hub hierarchy.
 *
 * Table: subjects
 *
 * Hierarchy: Department → Course → Subject → Resource
 *
 * Examples: Data Structures, Computer Networks, DBMS, Operating Systems
 */
@Entity
@Table(name = "subjects", indexes = {
    @Index(name = "idx_subject_course", columnList = "course_id"),
    @Index(name = "idx_subject_semester", columnList = "semester"),
    @Index(name = "idx_subject_code", columnList = "code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    /**
     * Semester number. Valid range: 1–10 (covers most UG/PG programs).
     */
    @Column(nullable = false)
    private Integer semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
