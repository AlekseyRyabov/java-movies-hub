package ru.practicum.moviehub.http.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.platform.commons.util.StringUtils;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.http.enums.HttpMethod;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

public abstract class BaseHttpHandler implements HttpHandler {

    private static final Set<HttpMethod> SUPPORTED_METHODS = Set.of(HttpMethod.values());
    private static final Gson GSON = new GsonBuilder().create();

    protected final MoviesStore moviesStore;

    public BaseHttpHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    protected Optional<ErrorResponse> validateMethod(HttpMethod method) {
        if (!SUPPORTED_METHODS.contains(method)) {
            return Optional.of(new ErrorResponse("Неподдерживаемый HTTP-метод"));
        }
        return Optional.empty();
    }

    protected void setHeaders(HttpExchange httpExchange) {
        Headers headers = httpExchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
    }

    protected void processResponse(int code, Object response, HttpExchange httpExchange) throws IOException {
        byte[] responseBytes = getResponseBytes(response);
        httpExchange.sendResponseHeaders(code, responseBytes.length);
        try (OutputStream os = httpExchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private byte[] getResponseBytes(Object object) {
        return GSON.toJson(object).getBytes(StandardCharsets.UTF_8);
    }

    protected Movie getMovieFromRequest(String requestBody) {
        return GSON.fromJson(requestBody, Movie.class);
    }

    protected Optional<ErrorResponse> validateRequestBody(String requestBody) {
        Movie movie = GSON.fromJson(requestBody, Movie.class);
        if (movie.getTitle() == null && movie.getYear() == 0) {
            return Optional.of(new ErrorResponse("Некорректный json"));
        }
        List<String> errorDetails = new ArrayList<>();
        if (StringUtils.isBlank(movie.getTitle())) {
            errorDetails.add("название не должно быть пустым");
        }
        if (movie.getTitle().length() > 100) {
            errorDetails.add("название слишком длинное");
        }
        if (movie.getYear() < 1888 || movie.getYear() > LocalDate.now().getYear()) {
            errorDetails.add("год должен быть между 1888 и 2026");
        }
        if (!errorDetails.isEmpty()) {
            return Optional.of(new ErrorResponse("Ошибка валидации", errorDetails));

        }
        return Optional.empty();
    }

}