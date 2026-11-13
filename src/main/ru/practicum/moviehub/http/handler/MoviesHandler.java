package ru.practicum.moviehub.http.handler;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.platform.commons.util.StringUtils;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.http.enums.HttpMethod;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

public class MoviesHandler extends BaseHttpHandler {

    public MoviesHandler(MoviesStore moviesStore) {
        super(moviesStore);
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        HttpMethod method;
        try {
            method = HttpMethod.valueOf(httpExchange.getRequestMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            setHeaders(httpExchange);
            processResponse(405, new ErrorResponse("Неподдерживаемый HTTP-метод"), httpExchange);
            return;
        }
        setHeaders(httpExchange);
        Optional<ErrorResponse> errorResponseOpt = validateMethod(method);
        if (errorResponseOpt.isPresent()) {
            processResponse(405, errorResponseOpt.get(), httpExchange);
            return;
        }
        switch (method) {
            case GET -> handleGetRequest(httpExchange);
            case POST -> handlePostRequest(httpExchange);
            case DELETE -> handleDeleteRequest(httpExchange);
        }
    }

    private void handleDeleteRequest(HttpExchange httpExchange) throws IOException {
        String path = httpExchange.getRequestURI().getPath();
        String[] pathParts = path.split("/");
        if (pathParts.length == 3) {
            String id = pathParts[2];
            try {
                UUID uuid = UUID.fromString(id);
                boolean isDelete = moviesStore.deleteByUuid(uuid);
                if (isDelete) {
                    processResponse(204, null, httpExchange);
                    return;
                }
                processResponse(404, new ErrorResponse("Фильм не найден"), httpExchange);
            } catch (IllegalArgumentException e) {
                processResponse(400, new ErrorResponse("Некорректный ID"), httpExchange);
            }
        } else {
            processResponse(405, new ErrorResponse("Неподдерживаемый HTTP-url"), httpExchange);
        }
    }

    private void handlePostRequest(HttpExchange httpExchange) throws IOException {
        String contentType = httpExchange.getRequestHeaders().getFirst("Content-type");
        if (StringUtils.isBlank(contentType) || !"application/json; charset=UTF-8".equals(contentType)) {
            processResponse(415, new ErrorResponse("Неподдерживаемый тип данных"), httpExchange);
        }
        String requestBody = new String(httpExchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Optional<ErrorResponse> errorResponseOpt = validateRequestBody(requestBody);
        if (errorResponseOpt.isPresent()) {
            processResponse(422, errorResponseOpt.get(), httpExchange);
            return;
        }
        Movie movie = getMovieFromRequest(requestBody);
        Movie result = moviesStore.addMovie(movie);
        processResponse(201, result, httpExchange);
    }

    private void handleGetRequest(HttpExchange httpExchange) throws IOException {
        String path = httpExchange.getRequestURI().getPath();
        String[] pathParts = path.split("/");
        if (pathParts.length == 3) {
            String id = pathParts[2];
            try {
                UUID uuid = UUID.fromString(id);
                Movie result = moviesStore.getByUuid(uuid);
                if (Objects.isNull(result)) {
                    processResponse(404, new ErrorResponse("Фильм не найден"), httpExchange);
                    return;
                }
                processResponse(200, result, httpExchange);
                return;
            } catch (IllegalArgumentException e) {
                processResponse(400, new ErrorResponse("Некорректный ID"), httpExchange);
                return;
            }
        } else if (pathParts.length == 2) {
            String params = httpExchange.getRequestURI().getQuery();
            if (!StringUtils.isBlank(params)) {
                String[] paramsParts = params.split("&");
                if (paramsParts.length == 1) {
                    String value = null;
                    try {
                        value = paramsParts[0].split("=")[1];
                        int year = Integer.parseInt(value);
                        List<Movie> movies = moviesStore.filterByYear(year);
                        processResponse(200, movies, httpExchange);
                        return;
                    } catch (NumberFormatException e) {
                        processResponse(
                            400,
                            new ErrorResponse(String.format("Некорректный параметр запроса — %s", value)),
                            httpExchange
                        );
                        return;
                    }
                }
            } else {
                List<Movie> movies = moviesStore.getAll();
                processResponse(200, movies, httpExchange);
                return;
            }
        }
        processResponse(405, new ErrorResponse("Неподдерживаемый HTTP-url"), httpExchange);
    }

}