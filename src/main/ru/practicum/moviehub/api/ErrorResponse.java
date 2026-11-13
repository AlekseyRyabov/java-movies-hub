package ru.practicum.moviehub.api;

import java.util.Collections;
import java.util.List;

/**
 * Дто ответа с ошибкой.
 */
public class ErrorResponse {

    /**
     * Ошибка.
     */
    private final String error;

    /**
     * Детали.
     */
    private final List<String> details;

    public ErrorResponse(String error, List<String> details) {
        this.error = error;
        this.details = details;
    }

    public ErrorResponse(String error) {
        this(error, Collections.emptyList());
    }

    public String getError() {
        return error;
    }

    public List<String> getDetails() {
        return details;
    }

}