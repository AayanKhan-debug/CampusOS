package com.campusos.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * UpdateProfileRequest — incoming JSON body for PUT /api/users/me
 *
 * All fields are optional — students update only what they want to change.
 * A null field means "don't change this field".
 * The service layer handles the partial-update logic.
 */
@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 100)
    private String department;

    private Integer year;

    @Size(max = 50)
    private String rollNumber;

    @Size(max = 20)
    private String phone;

    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;

    private String skills;

    private String interests;

    private String profilePicture;
}
