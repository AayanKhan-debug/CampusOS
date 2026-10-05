package com.campusos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user tries to perform an action they are not allowed to do.
 * Maps to HTTP 403 Forbidden.
 *
 * Distinguish from 401 Unauthorized:
 *   401 → you are not authenticated (not logged in)
 *   403 → you are authenticated but don't have permission
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
