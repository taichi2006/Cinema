package com.cinema.home;

import com.cinema.cinema.Cinema;
import com.cinema.cinema.CinemaDAO;
import com.cinema.cinema.CinemaService;
import com.cinema.cinema.DTO.Request.CinemaRequest;
import com.cinema.cinema.DTO.Response.CinemaResponse;
import com.cinema.movie.DTO.Request.MovieRequest;
import com.cinema.movie.DTO.Response.MovieResponse;
import com.cinema.movie.Genre;
import com.cinema.movie.Movie;
import com.cinema.movie.MovieDAO;
import com.cinema.movie.MovieService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeServiceTest {

    private static class StubMovieDAO extends MovieDAO {
        private List<Movie> stubNowShowing = Collections.emptyList();
        private List<Movie> stubComingSoon = Collections.emptyList();
        private List<Genre> stubGenres = Collections.emptyList();

        public void setStubMovies(List<Movie> nowShowing, List<Movie> comingSoon, List<Genre> genres) {
            this.stubNowShowing = nowShowing != null ? nowShowing : Collections.emptyList();
            this.stubComingSoon = comingSoon != null ? comingSoon : Collections.emptyList();
            this.stubGenres = genres != null ? genres : Collections.emptyList();
        }

        @Override
        public List<Movie> findMovies(MovieRequest filter) {
            if ("NOW_SHOWING".equalsIgnoreCase(filter.getStatus())) {
                return stubNowShowing;
            }
            if ("COMING_SOON".equalsIgnoreCase(filter.getStatus())) {
                return stubComingSoon;
            }
            return Collections.emptyList();
        }

        @Override
        public long countMovies(MovieRequest filter) {
            if ("NOW_SHOWING".equalsIgnoreCase(filter.getStatus())) {
                return stubNowShowing.size();
            }
            if ("COMING_SOON".equalsIgnoreCase(filter.getStatus())) {
                return stubComingSoon.size();
            }
            return 0L;
        }

        @Override
        public List<Genre> findAllGenres() {
            return stubGenres;
        }
    }

    private static class StubCinemaDAO extends CinemaDAO {
        private List<Cinema> stubCinemas = Collections.emptyList();
        private List<CinemaDAO.CityItem> stubCities = Collections.emptyList();

        public void setStubCinemas(List<Cinema> cinemas, List<CinemaDAO.CityItem> cities) {
            this.stubCinemas = cinemas != null ? cinemas : Collections.emptyList();
            this.stubCities = cities != null ? cities : Collections.emptyList();
        }

        @Override
        public List<Cinema> findCinemas(CinemaRequest filter) {
            return stubCinemas;
        }

        @Override
        public long countCinemas(CinemaRequest filter) {
            return stubCinemas.size();
        }

        @Override
        public List<CinemaDAO.CityItem> findDistinctCities() {
            return stubCities;
        }
    }

    private StubMovieDAO stubMovieDAO;
    private StubCinemaDAO stubCinemaDAO;
    private HomeService homeService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        stubMovieDAO = new StubMovieDAO();
        stubCinemaDAO = new StubCinemaDAO();
        MovieService movieService = new MovieService(stubMovieDAO);
        CinemaService cinemaService = new CinemaService(stubCinemaDAO);
        homeService = new HomeService(movieService, cinemaService);
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Lấy dữ liệu trang chủ thành công qua MovieService và CinemaService")
    void testGetHomeData_Success() throws Exception {
        Genre action = new Genre(1L, "ACTION", "Hành Động");
        Genre sciFi = new Genre(2L, "SCI_FI", "Khoa Học Viễn Tưởng");

        Movie nowShowingMovie = new Movie();
        nowShowingMovie.setMovieId(1L);
        nowShowingMovie.setTitle("Dune: Hành Tinh Cát - Phần Hai");
        nowShowingMovie.setDescription("Mô tả Dune");
        nowShowingMovie.setDurationMinutes(166);
        nowShowingMovie.setReleaseDate(LocalDate.of(2026, 3, 1));
        nowShowingMovie.setPosterUrl("https://example.com/posters/dune2.jpg");
        nowShowingMovie.setLanguage("VI");
        nowShowingMovie.setDefaultFormat("2D");
        nowShowingMovie.setAgeRating("T16");
        nowShowingMovie.setAgeLimit(16);
        nowShowingMovie.setStatus("NOW_SHOWING");
        nowShowingMovie.setGenres(List.of(sciFi));

        Movie comingSoonMovie = new Movie();
        comingSoonMovie.setMovieId(2L);
        comingSoonMovie.setTitle("Deadpool & Wolverine");
        comingSoonMovie.setDescription("Mô tả Deadpool");
        comingSoonMovie.setDurationMinutes(128);
        comingSoonMovie.setReleaseDate(LocalDate.of(2026, 11, 20));
        comingSoonMovie.setPosterUrl("https://example.com/posters/deadpool3.jpg");
        comingSoonMovie.setLanguage("VI");
        comingSoonMovie.setDefaultFormat("2D");
        comingSoonMovie.setAgeRating("T18");
        comingSoonMovie.setAgeLimit(18);
        comingSoonMovie.setStatus("COMING_SOON");
        comingSoonMovie.setGenres(List.of(action));

        Cinema featuredCinema = new Cinema(
                1L,
                "Galaxy Nguyễn Du",
                "116 Nguyễn Du, Quận 1, TP.HCM",
                "HCM",
                "Hồ Chí Minh",
                "028 3823 4567",
                "https://example.com/cinema1.jpg",
                10.7725,
                106.698,
                "ACTIVE"
        );

        CinemaDAO.CityItem city = new CinemaDAO.CityItem("HCM", "Hồ Chí Minh");

        stubMovieDAO.setStubMovies(List.of(nowShowingMovie), List.of(comingSoonMovie), List.of(action, sciFi));
        stubCinemaDAO.setStubCinemas(List.of(featuredCinema), List.of(city));

        Map<String, Object> result = homeService.getHomeData();

        assertNotNull(result);
        assertEquals(true, result.get("success"));
        assertNotNull(result.get("data"));
        assertEquals(Collections.emptyMap(), result.get("meta"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");

        @SuppressWarnings("unchecked")
        List<MovieResponse> nowShowing = (List<MovieResponse>) data.get("nowShowing");
        assertEquals(1, nowShowing.size());
        assertEquals("Dune: Hành Tinh Cát - Phần Hai", nowShowing.get(0).getTitle());
        assertEquals("NOW_SHOWING", nowShowing.get(0).getStatus());

        @SuppressWarnings("unchecked")
        List<MovieResponse> comingSoon = (List<MovieResponse>) data.get("comingSoon");
        assertEquals(1, comingSoon.size());
        assertEquals("Deadpool & Wolverine", comingSoon.get(0).getTitle());
        assertEquals("COMING_SOON", comingSoon.get(0).getStatus());

        @SuppressWarnings("unchecked")
        List<CinemaResponse> featuredCinemas = (List<CinemaResponse>) data.get("featuredCinemas");
        assertEquals(1, featuredCinemas.size());
        assertEquals("Galaxy Nguyễn Du", featuredCinemas.get(0).getName());

        @SuppressWarnings("unchecked")
        List<Map<String, String>> genres = (List<Map<String, String>>) data.get("genres");
        assertEquals(2, genres.size());
        assertEquals("ACTION", genres.get(0).get("code"));
        assertEquals("Hành Động", genres.get(0).get("name"));

        @SuppressWarnings("unchecked")
        List<Map<String, String>> cities = (List<Map<String, String>>) data.get("cities");
        assertEquals(1, cities.size());
        assertEquals("HCM", cities.get(0).get("code"));
        assertEquals("Hồ Chí Minh", cities.get(0).get("name"));

        // Kiểm tra serialization JSON
        String json = objectMapper.writeValueAsString(result);
        JsonNode root = objectMapper.readTree(json);
        assertTrue(root.get("success").asBoolean());
        assertTrue(root.get("data").has("nowShowing"));
        assertTrue(root.get("data").has("comingSoon"));
        assertTrue(root.get("data").has("featuredCinemas"));
        assertTrue(root.get("data").has("genres"));
        assertTrue(root.get("data").has("cities"));
    }

    @Test
    @DisplayName("Dữ liệu trang chủ khi chưa có dữ liệu: các mảng trả về rỗng, không bị null")
    void testGetHomeData_EmptyLists() {
        stubMovieDAO.setStubMovies(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        stubCinemaDAO.setStubCinemas(Collections.emptyList(), Collections.emptyList());

        Map<String, Object> result = homeService.getHomeData();

        assertNotNull(result);
        assertEquals(true, result.get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertNotNull(data.get("nowShowing"));
        assertTrue(((List<?>) data.get("nowShowing")).isEmpty());

        assertNotNull(data.get("comingSoon"));
        assertTrue(((List<?>) data.get("comingSoon")).isEmpty());

        assertNotNull(data.get("featuredCinemas"));
        assertTrue(((List<?>) data.get("featuredCinemas")).isEmpty());

        assertNotNull(data.get("genres"));
        assertTrue(((List<?>) data.get("genres")).isEmpty());

        assertNotNull(data.get("cities"));
        assertTrue(((List<?>) data.get("cities")).isEmpty());
    }

    @Test
    @DisplayName("Cấu trúc JSON tuần tự các trường khớp chính xác đặc tả OpenAPI")
    void testGetHomeData_JsonStructureOrder() throws Exception {
        stubMovieDAO.setStubMovies(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        stubCinemaDAO.setStubCinemas(Collections.emptyList(), Collections.emptyList());

        Map<String, Object> result = homeService.getHomeData();
        String json = objectMapper.writeValueAsString(result);
        JsonNode root = objectMapper.readTree(json);
        JsonNode data = root.get("data");

        List<String> fieldNames = new ArrayList<>();
        data.fieldNames().forEachRemaining(fieldNames::add);

        assertEquals(List.of("nowShowing", "comingSoon", "featuredCinemas", "genres", "cities"), fieldNames);
    }
}
