package com.evplanner.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class JourneyApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException exception) {
        String message = exception.getMessage() == null
                ? "Invalid journey request"
                : exception.getMessage();

        HttpStatus status = message.startsWith("Vehicle not found:")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setTitle(status == HttpStatus.NOT_FOUND
                ? "Vehicle not found"
                : "Invalid journey request");
        return problem;
    }
}
