package com.campusos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the request is syntactically valid but semantically wrong.
 * Maps to HTTP 400 Bad Request.
 *
 * Example: Trying to log in with incorrect credentials.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
