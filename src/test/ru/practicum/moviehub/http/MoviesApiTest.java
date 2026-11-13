package ru.practicum.moviehub.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

public class MoviesApiTest {

    private static final MoviesServer moviesServer = new MoviesServer(new MoviesStore(), 8080);
    private static final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private static final HttpResponse.BodyHandler<String> BODY_HANDLER =
        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
    private static final Gson GSON = new GsonBuilder().create();

    @BeforeAll
    static void beforeAll() {
        moviesServer.start();
    }

    @BeforeEach
    void beforeEach() {
        moviesServer.clearStore();
    }

    @AfterAll
    static void afterAll() {
        moviesServer.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/movies"))
            .GET()
            .build();

        HttpResponse<String> resp = httpClient.send(req, BODY_HANDLER);

        assertBase(resp, 200);

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                   "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenNonEmpty_returnsArray() throws Exception {
        Movie movie = new Movie("Матрица", 1999);
        HttpRequest addMovie = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/movies"))
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(movie)))
            .header("Content-type", "application/json; charset=UTF-8")
            .build();

        httpClient.send(addMovie, BODY_HANDLER);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/movies"))
            .GET()
            .build();

        HttpResponse<String> resp = httpClient.send(req, BODY_HANDLER);

        assertBase(resp, 200);

        String body = resp.body().trim();
        List<Movie> movies = GSON.fromJson(body, new ListOfMoviesTypeToken().getType());
        assertEquals(1, movies.size());
    }

    @Test
    void postMovies_whenValidRequest() throws Exception {
        Movie movie = new Movie("Матрица", 1999);
        HttpRequest addMovie = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/movies"))
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(movie)))
            .header("Content-type", "application/json; charset=UTF-8")
            .build();

        HttpResponse<String> resp = httpClient.send(addMovie, BODY_HANDLER);

        assertBase(resp, 201);

        String body = resp.body().trim();
        Movie result = GSON.fromJson(body,Movie.class);
        assertEquals(1999, result.getYear());
        assertEquals("Матрица", result.getTitle());
        assertNotNull(result.getId());
    }

    @Test
    void postMovies_whenEmptyTitle_returnsError() throws Exception {
        Movie movie = new Movie("", 1999);
        HttpRequest addMovie = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/movies"))
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(movie)))
            .header("Content-type", "application/json; charset=UTF-8")
            .build();

        HttpResponse<String> resp = httpClient.send(addMovie, BODY_HANDLER);

        assertBase(resp, 422);

        String body = resp.body().trim();
        ErrorResponse result = GSON.fromJson(body, ErrorResponse.class);
        assertEquals("Ошибка валидации", result.getError());
        assertEquals("название не должно быть пустым", result.getDetails().getFirst());
    }

    @Test
    void postMovies_whenLongTitle_returnsError() throws Exception {
        Movie movie = new Movie(
            "ОченьДлинноеНазваниеФильмаПроОченьОченьДалекоеКоролевствоГдеЖилиОченьДолгоИОченьСчастливоКорольИКоролева",
            1999
        );
        HttpRequest addMovie = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/movies"))
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(movie)))
            .header("Content-type", "application/json; charset=UTF-8")
            .build();

        HttpResponse<String> resp = httpClient.send(addMovie, BODY_HANDLER);

        assertBase(resp, 422);

        String body = resp.body().trim();
        ErrorResponse result = GSON.fromJson(body, ErrorResponse.class);
        assertEquals("Ошибка валидации", result.getError());
        assertEquals("название слишком длинное", result.getDetails().getFirst());
    }

    @Test
    void postMovies_whenInvalidYear_returnsError() throws Exception {
        Movie movie = new Movie("Матрица", 0);
        HttpRequest addMovie = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/movies"))
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(movie)))
            .header("Content-type", "application/json; charset=UTF-8")
            .build();

        HttpResponse<String> resp = httpClient.send(addMovie, BODY_HANDLER);

        assertBase(resp, 422);

        String body = resp.body().trim();
        ErrorResponse result = GSON.fromJson(body, ErrorResponse.class);
        assertEquals("Ошибка валидации", result.getError());
        assertEquals("год должен быть между 1888 и 2026", result.getDetails().getFirst());
    }

    @Test
    void postMovies_whenInvalidRequest_returnsError() throws Exception {
        Movie movie = new Movie("", 0);
        HttpRequest addMovie = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/movies"))
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(movie)))
            .header("Content-type", "application/json; charset=UTF-8")
            .build();

        HttpResponse<String> resp = httpClient.send(addMovie, BODY_HANDLER);

        assertBase(resp, 422);

        String body = resp.body().trim();
        ErrorResponse result = GSON.fromJson(body, ErrorResponse.class);
        assertEquals("Ошибка валидации", result.getError());
        assertEquals(2, result.getDetails().size());
    }

    @Test
    void postMovies_whenInvalidRequestContentType_returnsError() throws Exception {
        Movie movie = new Movie("Матрица", 1999);
        HttpRequest addMovie = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/movies"))
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(movie)))
            .header("Content-type", "application/xml; charset=UTF-8")
            .build();

        HttpResponse<String> resp = httpClient.send(addMovie, BODY_HANDLER);

        assertBase(resp, 415);

        String body = resp.body().trim();
        ErrorResponse result = GSON.fromJson(body, ErrorResponse.class);
        assertEquals("Неподдерживаемый тип данных", result.getError());
    }

    @Test
    void postMovies_whenInvalidJson_returnsError() throws Exception {
        ErrorResponse movie = new ErrorResponse("Матрица", Collections.emptyList());
        HttpRequest addMovie = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/movies"))
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(movie)))
            .header("Content-type", "application/json; charset=UTF-8")
            .build();

        HttpResponse<String> resp = httpClient.send(addMovie, BODY_HANDLER);

        assertBase(resp, 422);

        String body = resp.body().trim();
        ErrorResponse result = GSON.fromJson(body, ErrorResponse.class);
        assertEquals("Некорректный json", result.getError());
    }

    @Test
    void getMovieById_whenExists() throws Exception {
        Movie movie = new Movie("Матрица", 1999);
        HttpRequest addMovie = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/movies"))
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(movie)))
            .header("Content-type", "application/json; charset=UTF-8")
            .build();

        HttpResponse<String> response = httpClient.send(addMovie, BODY_HANDLER);
        Movie addResult = GSON.fromJson(response.body(), Movie.class);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(String.format("http://localhost:8080/movies/%s", addResult.getId())))
            .GET()
            .build();

        HttpResponse<String> resp = httpClient.send(req, BODY_HANDLER);
        assertBase(resp, 200);
        Movie getResult = GSON.fromJson(response.body(), Movie.class);
        assertEquals(addResult.getId(), getResult.getId());
        assertEquals(addResult.getTitle(), getResult.getTitle());
        assertEquals(addResult.getYear(), getResult.getYear());
    }

    @Test
    void getMovieById_whenNonExists() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(String.format("http://localhost:8080/movies/%s", UUID.randomUUID())))
            .GET()
            .build();

        HttpResponse<String> resp = httpClient.send(req, BODY_HANDLER);
        assertBase(resp, 404);
        ErrorResponse getResult = GSON.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Фильм не найден", getResult.getError());
    }

    @Test
    void getMovieById_whenIncorrectId() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(String.format("http://localhost:8080/movies/%s", 1)))
            .GET()
            .build();

        HttpResponse<String> resp = httpClient.send(req, BODY_HANDLER);
        assertBase(resp, 400);
        ErrorResponse getResult = GSON.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Некорректный ID", getResult.getError());
    }

    @Test
    void deleteMovieById_whenExists() throws Exception {
        Movie movie = new Movie("Матрица", 1999);
        HttpRequest addMovie = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/movies"))
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(movie)))
            .header("Content-type", "application/json; charset=UTF-8")
            .build();

        HttpResponse<String> response = httpClient.send(addMovie, BODY_HANDLER);
        Movie addResult = GSON.fromJson(response.body(), Movie.class);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(String.format("http://localhost:8080/movies/%s", addResult.getId())))
            .DELETE()
            .build();

        HttpResponse<String> resp = httpClient.send(req, BODY_HANDLER);
        assertBase(resp, 204);
        assertEquals("", resp.body());
    }

    @Test
    void deleteMovieById_whenNonExists() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(String.format("http://localhost:8080/movies/%s", UUID.randomUUID())))
            .DELETE()
            .build();

        HttpResponse<String> resp = httpClient.send(req, BODY_HANDLER);
        assertBase(resp, 404);
        ErrorResponse getResult = GSON.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Фильм не найден", getResult.getError());
    }

    @Test
    void deleteMovieById_whenIncorrectId() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(String.format("http://localhost:8080/movies/%s", 1)))
            .DELETE()
            .build();

        HttpResponse<String> resp = httpClient.send(req, BODY_HANDLER);
        assertBase(resp, 400);
        ErrorResponse getResult = GSON.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Некорректный ID", getResult.getError());
    }

    @Test
    void filterMoviesByYear_whenExists() throws Exception {
        Movie movie = new Movie("Матрица", 1999);
        HttpRequest addMovie = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/movies"))
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(movie)))
            .header("Content-type", "application/json; charset=UTF-8")
            .build();

        httpClient.send(addMovie, BODY_HANDLER);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(String.format("http://localhost:8080/movies?year=%d", 1999)))
            .GET()
            .build();

        HttpResponse<String> resp = httpClient.send(req, BODY_HANDLER);
        assertBase(resp, 200);
        List<Movie> result = GSON.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(1, result.size());
        assertEquals(1999, result.getFirst().getYear());
    }

    @Test
    void filterMoviesByYear_whenNonExists() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(String.format("http://localhost:8080/movies?year=%d", 1999)))
            .GET()
            .build();

        HttpResponse<String> resp = httpClient.send(req, BODY_HANDLER);
        assertBase(resp, 200);
        List<Movie> result = GSON.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertTrue(result.isEmpty());
    }

    @Test
    void filterMovieById_whenIncorrectId() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(String.format("http://localhost:8080/movies?year=%s", "Матрица")))
            .GET()
            .build();

        HttpResponse<String> resp = httpClient.send(req, BODY_HANDLER);
        assertBase(resp, 400);
        ErrorResponse getResult = GSON.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Некорректный параметр запроса — Матрица", getResult.getError());
    }

    @Test
    void unsupportedHttpMethod() throws Exception {
        Movie movie = new Movie("Матрица", 1999);
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/movies"))
            .PUT(HttpRequest.BodyPublishers.ofString(GSON.toJson(movie)))
            .build();

        HttpResponse<String> resp = httpClient.send(req, BODY_HANDLER);
        assertBase(resp, 405);
        ErrorResponse getResult = GSON.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Неподдерживаемый HTTP-метод", getResult.getError());
    }

    private void assertBase(HttpResponse<String> resp, int code) {
        assertEquals(code, resp.statusCode(), String.format("Метод должен вернуть %d", code));

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals(
            "application/json; charset=UTF-8", contentTypeHeaderValue,
            "Content-Type должен содержать формат данных и кодировку"
        );
    }
}