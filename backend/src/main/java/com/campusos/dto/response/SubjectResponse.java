package com.campusos.dto.response;

import com.campusos.entity.Subject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubjectResponse {

    private Long id;
    private String name;
    private String code;
    private Integer semester;
    private Long courseId;
    private String courseName;
    private Long departmentId;
    private String departmentName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SubjectResponse fromEntity(Subject subject) {
        return SubjectResponse.builder()
                .id(subject.getId())
                .name(subject.getName())
                .code(subject.getCode())
                .semester(subject.getSemester())
                .courseId(subject.getCourse() != null ? subject.getCourse().getId() : null)
                .courseName(subject.getCourse() != null ? subject.getCourse().getName() : null)
                .departmentId(subject.getCourse() != null && subject.getCourse().getDepartment() != null
                        ? subject.getCourse().getDepartment().getId() : null)
                .departmentName(subject.getCourse() != null && subject.getCourse().getDepartment() != null
                        ? subject.getCourse().getDepartment().getName() : null)
                .description(subject.getDescription())
                .createdAt(subject.getCreatedAt())
                .updatedAt(subject.getUpdatedAt())
                .build();
    }
}
