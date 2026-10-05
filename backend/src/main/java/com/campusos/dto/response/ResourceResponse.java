package com.campusos.dto.response;

import com.campusos.entity.Resource;
import com.campusos.enums.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ResourceResponse {

    private Long id;
    private String title;
    private String description;
    private ResourceType resourceType;
    private String url;
    private Long subjectId;
    private String subjectName;
    private Long courseId;
    private String courseName;
    private Long departmentId;
    private String departmentName;
    private UserResponse uploadedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ResourceResponse fromEntity(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .title(resource.getTitle())
                .description(resource.getDescription())
                .resourceType(resource.getResourceType())
                .url(resource.getUrl())
                .subjectId(resource.getSubject() != null ? resource.getSubject().getId() : null)
                .subjectName(resource.getSubject() != null ? resource.getSubject().getName() : null)
                .courseId(resource.getSubject() != null && resource.getSubject().getCourse() != null
                        ? resource.getSubject().getCourse().getId() : null)
                .courseName(resource.getSubject() != null && resource.getSubject().getCourse() != null
                        ? resource.getSubject().getCourse().getName() : null)
                .departmentId(resource.getSubject() != null
                        && resource.getSubject().getCourse() != null
                        && resource.getSubject().getCourse().getDepartment() != null
                        ? resource.getSubject().getCourse().getDepartment().getId() : null)
                .departmentName(resource.getSubject() != null
                        && resource.getSubject().getCourse() != null
                        && resource.getSubject().getCourse().getDepartment() != null
                        ? resource.getSubject().getCourse().getDepartment().getName() : null)
                .uploadedBy(resource.getUploadedBy() != null
                        ? UserResponse.fromEntity(resource.getUploadedBy()) : null)
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .build();
    }
}
