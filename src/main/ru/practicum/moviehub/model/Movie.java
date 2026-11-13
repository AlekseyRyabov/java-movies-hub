package ru.practicum.moviehub.model;

import java.util.UUID;

/**
 * Фильм.
 */
public class Movie {

    /**
     * Уникальный идентификатор.
     */
    private final UUID id;

    /**
     * Год выхода.
     */
    private final int year;

    /**
     * Название фильма.
     */
    private final String title;

    public Movie(UUID id, String title, int year) {
        this.id = id;
        this.title = title;
        this.year = year;
    }

    public Movie(String title, int year) {
        this.id = null;
        this.title = title;
        this.year = year;
    }

    public Movie(UUID id, Movie movie) {
        this.id = id;
        this.title = movie.getTitle();
        this.year = movie.getYear();
    }

    public UUID getId() {
        return id;
    }

    public int getYear() {
        return year;
    }

    public String getTitle() {
        return title;
    }
}