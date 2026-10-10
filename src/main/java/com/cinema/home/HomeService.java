package com.cinema.home;

import com.cinema.cinema.CinemaDAO.CityItem;
import com.cinema.cinema.CinemaService;
import com.cinema.cinema.DTO.Request.CinemaRequest;
import com.cinema.movie.DTO.Request.MovieRequest;
import com.cinema.movie.Genre;
import com.cinema.movie.MovieService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HomeService {

    private static final int DEFAULT_LIMIT = 10;

    private final MovieService movieService;
    private final CinemaService cinemaService;

    public HomeService() {
        this(new MovieService(), new CinemaService());
    }

    public HomeService(MovieService movieService, CinemaService cinemaService) {
        this.movieService = movieService;
        this.cinemaService = cinemaService;
    }

    public Map<String, Object> getHomeData() {
        // 1. Phim đang chiếu (gọi từ MovieService)
        List<?> nowShowing = movieService.getMovies(
                new MovieRequest(null, null, "NOW_SHOWING", 0, DEFAULT_LIMIT, "releaseDate,desc")
        ).getItems();

        // 2. Phim sắp chiếu (gọi từ MovieService)
        List<?> comingSoon = movieService.getMovies(
                new MovieRequest(null, null, "COMING_SOON", 0, DEFAULT_LIMIT, "releaseDate,asc")
        ).getItems();

        // 3. Rạp nổi bật (gọi từ CinemaService)
        List<?> featuredCinemas = cinemaService.getCinemas(
                new CinemaRequest(null, null, 0, DEFAULT_LIMIT, "name,asc")
        ).getItems();

        // 4. Thể loại phim (gọi từ MovieService)
        List<Genre> rawGenres = movieService.getGenres();
        List<Map<String, String>> genres = new ArrayList<>();
        if (rawGenres != null) {
            for (Genre g : rawGenres) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("code", String.valueOf(g.getGenreId()));
                item.put("name", g.getGenreName());
                genres.add(item);
            }
        }

        // 5. Thành phố (gọi từ CinemaService)
        List<CityItem> rawCities = cinemaService.getCities();
        List<Map<String, String>> cities = new ArrayList<>();
        if (rawCities != null) {
            for (CityItem c : rawCities) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("code", c.cityCode());
                item.put("name", c.cityName());
                cities.add(item);
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("nowShowing", nowShowing);
        data.put("comingSoon", comingSoon);
        data.put("featuredCinemas", featuredCinemas);
        data.put("genres", genres);
        data.put("cities", cities);

        return data;
    }
}
