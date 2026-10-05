package com.campusos.dto.request;

import com.campusos.enums.ResourceType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateResourceRequest {

    @Size(min = 2, max = 200, message = "Title must be between 2 and 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private ResourceType resourceType;

    @Size(max = 500, message = "URL must not exceed 500 characters")
    private String url;

    private Long subjectId;
}
