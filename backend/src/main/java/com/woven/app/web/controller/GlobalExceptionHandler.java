package com.woven.app.web.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadBody(MethodArgumentNotValidException ex) {
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        fe -> fe.getField(),
                        Collectors.mapping(fe -> fe.getDefaultMessage(), Collectors.toList())
                ));
        return Map.of("message", "Validation failed", "errors", fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleConstraintViolations(ConstraintViolationException ex) {
        return Map.of("message", "Validation failed", "errors",
                ex.getConstraintViolations().stream().map(v -> Map.of(
                        "property", v.getPropertyPath().toString(),
                        "message", v.getMessage()
                )).toList()
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(EntityNotFoundException ex) {
        return Map.of("message", ex.getMessage());
    }
}
