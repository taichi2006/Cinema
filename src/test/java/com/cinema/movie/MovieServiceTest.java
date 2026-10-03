package com.cinema.movie;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MovieServiceTest {

    private MovieService movieService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Stub DAO for testing validation & service layer logic without database dependency
    private static class StubMovieDAO extends MovieDAO {
        private List<Movie> stubMovies = Collections.emptyList();
        private long stubCount = 0L;

        public void setStubData(List<Movie> movies, long count) {
            this.stubMovies = movies;
            this.stubCount = count;
        }

        @Override
        public List<Movie> findMovies(MovieFilter filter) {
            return stubMovies;
        }

        @Override
        public long countMovies(MovieFilter filter) {
            return stubCount;
        }
    }

    private StubMovieDAO stubDAO;

    @BeforeEach
    void setUp() {
        stubDAO = new StubMovieDAO();
        movieService = new MovieService(stubDAO);
    }

    @Test
    @DisplayName("Ném InvalidFilterException khi q vượt quá 100 ký tự")
    void testQueryTooLong() {
        String longQuery = "a".repeat(101);
        MovieFilter filter = new MovieFilter(longQuery, null, null, 1, 10, "releaseDate,desc");

        InvalidFilterException exception = assertThrows(
                InvalidFilterException.class,
                () -> movieService.getMovies(filter)
        );
        assertEquals("Từ khóa tìm kiếm 'q' không được vượt quá 100 ký tự.", exception.getMessage());
    }

    @Test
    @DisplayName("Ném InvalidFilterException khi status không hợp lệ")
    void testInvalidStatus() {
        MovieFilter filter = new MovieFilter(null, null, "INVALID_STATUS", 1, 10, "releaseDate,desc");

        InvalidFilterException exception = assertThrows(
                InvalidFilterException.class,
                () -> movieService.getMovies(filter)
        );
        assertEquals(
                "Trạng thái 'status' không hợp lệ: INVALID_STATUS. Các giá trị hợp lệ: [NOW_SHOWING, COMING_SOON, ENDED]",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Ném InvalidFilterException khi sort không hợp lệ")
    void testInvalidSort() {
        MovieFilter filter = new MovieFilter(null, null, null, 1, 10, "unknown_field,asc");

        InvalidFilterException exception = assertThrows(
                InvalidFilterException.class,
                () -> movieService.getMovies(filter)
        );
        assertEquals(
                "Tham số 'sort' không hợp lệ: unknown_field,asc. Các giá trị hợp lệ: [releaseDate,asc, releaseDate,desc, title,asc, title,desc]",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Ném InvalidFilterException khi page < 1")
    void testInvalidPage() {
        MovieFilter filter = new MovieFilter(null, null, null, 0, 10, "releaseDate,desc");

        InvalidFilterException exception = assertThrows(
                InvalidFilterException.class,
                () -> movieService.getMovies(filter)
        );
        assertEquals("Số trang 'page' phải lớn hơn hoặc bằng 1.", exception.getMessage());
    }

    @Test
    @DisplayName("Ném InvalidFilterException khi size < 1 hoặc > 50")
    void testInvalidSize() {
        MovieFilter filterNegativeSize = new MovieFilter(null, null, null, 1, 0, "releaseDate,desc");
        assertThrows(
                InvalidFilterException.class,
                () -> movieService.getMovies(filterNegativeSize)
        );

        MovieFilter filterTooLargeSize = new MovieFilter(null, null, null, 1, 51, "releaseDate,desc");
        assertThrows(
                InvalidFilterException.class,
                () -> movieService.getMovies(filterTooLargeSize)
        );
    }

    @Test
    @DisplayName("Ném InvalidFilterException khi page=2147483647&size=50 gây tràn số offset")
    void testPageMaxIntOverflow() {
        MovieFilter filter = new MovieFilter(null, null, null, 2147483647, 50, "releaseDate,desc");

        InvalidFilterException exception = assertThrows(
                InvalidFilterException.class,
                () -> movieService.getMovies(filter)
        );
        assertEquals("Vị trí phân trang vượt quá giới hạn cho phép.", exception.getMessage());
    }

    @Test
    @DisplayName("JSON hợp lệ và không có thuộc tính rawReleaseDate")
    void testJsonNoRawReleaseDate() throws Exception {
        Movie movie = new Movie();
        movie.setMovieId(1L);
        movie.setTitle("Mai");
        movie.setDurationMinutes(120);
        movie.setReleaseDate(LocalDate.of(2024, 2, 10));
        movie.setStatus("NOW_SHOWING");

        stubDAO.setStubData(List.of(movie), 1L);

        MovieFilter filter = new MovieFilter(null, null, null, 1, 10, null);
        SuccessEnvelope<List<MovieSummary>> response = movieService.getMovies(filter);

        String json = objectMapper.writeValueAsString(response);
        JsonNode root = objectMapper.readTree(json);
        JsonNode firstMovie = root.get("data").get(0);

        assertTrue(firstMovie.has("releaseDate"), "Phải có thuộc tính releaseDate");
        assertEquals("2024-02-10", firstMovie.get("releaseDate").asText());
        assertFalse(firstMovie.has("rawReleaseDate"), "Tuyệt đối không có thuộc tính rawReleaseDate");
    }

    @Test
    @DisplayName("Lọc phim có nhiều thể loại: phim không trùng và trả đủ thể loại không trùng lặp")
    void testMovieWithMultipleGenres() {
        Movie movie = new Movie();
        movie.setMovieId(1L);
        movie.setTitle("Dune: Part Two");
        movie.setDurationMinutes(166);
        movie.setReleaseDate(LocalDate.of(2024, 3, 1));
        movie.setStatus("NOW_SHOWING");

        Genre genre1 = new Genre(1L, "SCI_FI", "Khoa học viễn tưởng");
        Genre genre2 = new Genre(2L, "ACTION", "Hành động");
        Genre duplicateGenre = new Genre(1L, "SCI_FI", "Khoa học viễn tưởng");

        movie.setGenres(List.of(genre1, genre2, duplicateGenre));

        stubDAO.setStubData(List.of(movie), 1L);

        MovieFilter filter = new MovieFilter(null, "Khoa học viễn tưởng", null, 1, 10, null);
        SuccessEnvelope<List<MovieSummary>> response = movieService.getMovies(filter);

        assertEquals(1, response.getData().size());
        MovieSummary summary = response.getData().get(0);
        assertEquals(2, summary.getGenres().size(), "Các thể loại trùng lặp phải được lọc sạch");
        assertEquals(List.of("Khoa học viễn tưởng", "Hành động"), summary.getGenres());
    }

    @Test
    @DisplayName("Tìm không có kết quả: 200, data: [], tổng số phần tử bằng 0")
    void testNoResultsFound() {
        stubDAO.setStubData(Collections.emptyList(), 0L);

        MovieFilter filter = new MovieFilter("KhongTonTai123456", null, null, 1, 10, null);
        SuccessEnvelope<List<MovieSummary>> response = assertDoesNotThrow(
                () -> movieService.getMovies(filter)
        );

        assertNotNull(response);
        assertNotNull(response.getData());
        assertTrue(response.getData().isEmpty(), "data phải là mảng rỗng []");
        assertEquals(0L, response.getMeta().getTotalElements(), "tổng số phần tử bằng 0");
        assertEquals(0, response.getMeta().getTotalPages());
    }
}
