package ru.practicum.moviehub.store;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ru.practicum.moviehub.model.Movie;

/**
 * Сервис-хранилище фильмов.
 */
public class MoviesStore {

    /**
     * Карта uuid-фильм.
     */
    private final Map<UUID, Movie> movieMap = new HashMap<>();

    public MoviesStore() {

    }

    /**
     * Возвращает все фильмы в хранилище.
     */
    public List<Movie> getAll() {
        return List.copyOf(movieMap.values());
    }

    /**
     * Возращает конретный добавленный фильм.
     * @param id уникальный uuid фильма.
     */
    public Movie getByUuid(UUID id) {
        return movieMap.get(id);
    }

    /**
     * Добавляет фильм в хранилище.
     * @param movie - фильм.
     */
    public Movie addMovie(Movie movie) {
        UUID uuid = UUID.randomUUID();
        Movie result = new Movie(uuid, movie);
        movieMap.put(uuid, movie);
        return result;
    }

    /**
     * Обнуление данных в хранилище.
     */
    public void clear() {
        movieMap.clear();
    }

    /**
     * Удаление конкретного фильма из хранилища.
     * @param id уникальный uuid фильма.
     * @return true - если удален, false - если не найден.
     */
    public boolean deleteByUuid(UUID id) {
        return movieMap.remove(id) != null;
    }

    /**
     * Возвращает фильмы, подходящие под указанный год.
     * @param year год выхода фильма.
     */
    public List<Movie> filterByYear(int year) {
        return movieMap.values().stream()
            .filter(movie -> year == movie.getYear())
            .toList();
    }
}