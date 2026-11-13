package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import ru.practicum.moviehub.http.handler.MoviesHandler;
import ru.practicum.moviehub.store.MoviesStore;

/**
 * Http-сервер с хранением фильмов в памяти.
 */
public class MoviesServer {

    private final HttpServer server;
    private final MoviesStore moviesStore;

    public MoviesServer(MoviesStore moviesStore, int port) {
        this.moviesStore = moviesStore;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/movies", new MoviesHandler(moviesStore));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    /**
     * Старт сервера.
     */
    public void start() {
        server.start();
        System.out.println("Сервер запущен");
    }

    /**
     * Остановка сервера.
     */
    public void stop() {
        server.stop(0);
        System.out.println("Сервер остановлен");
    }

    /**
     * Очистка хранилища.
     */
    public void clearStore() {
        moviesStore.clear();
    }

}