package com.campusos.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * RegisterRequest — incoming JSON body for POST /api/auth/register
 *
 * @NotBlank  → field must not be null, empty, or whitespace-only
 * @Email     → validates email format
 * @Size      → enforces length limits
 *
 * These annotations are checked by Spring's @Valid before the method body runs.
 * If validation fails, Spring throws a MethodArgumentNotValidException which
 * our GlobalExceptionHandler converts into a clean 422 JSON error response.
 */
@Data   // Lombok: generates getters, setters, equals, hashCode, toString
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // Optional at registration — students can fill these in later
    private String department;
    private Integer year;
    private String rollNumber;
    private String phone;
}
