package com.woven.app.web.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice(assignableTypes = {LifecycleController.class, GovernanceAdminController.class, SuggestionController.class, ChangeTrackingController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LifecycleExceptionHandler {
    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> forbidden() { return Map.of("message", "You do not have access to this workflow action."); }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> missing() { return Map.of("message", "Workflow record or required configuration was not found."); }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> conflict(IllegalStateException error) { return Map.of("message", error.getMessage()); }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalid(IllegalArgumentException error) { return Map.of("message", error.getMessage()); }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> persistenceConflict() {
        return Map.of("message", "The workflow could not be saved. Refresh its state and retry with the same command ID.");
    }
}
