package com.cinema.home;

import com.cinema.cinema.dao.CinemaDAO.CityItem;
import com.cinema.cinema.dto.request.CinemaRequest;
import com.cinema.cinema.service.CinemaService;
import com.cinema.movie.dto.request.MovieRequest;
import com.cinema.movie.entity.Genre;
import com.cinema.movie.service.MovieService;

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
        Map<String, Object> nowShowingResult = movieService.getMovies(
                new MovieRequest(null, null, "NOW_SHOWING", 0, DEFAULT_LIMIT, "releaseDate,desc")
        );
        Object nowShowing = extractMovieItems(nowShowingResult);

        // 2. Phim sắp chiếu (gọi từ MovieService)
        Map<String, Object> comingSoonResult = movieService.getMovies(
                new MovieRequest(null, null, "COMING_SOON", 0, DEFAULT_LIMIT, "releaseDate,asc")
        );
        Object comingSoon = extractMovieItems(comingSoonResult);

        // 3. Rạp nổi bật (gọi từ CinemaService)
        Map<String, Object> cinemaResult = cinemaService.getCinemas(
                new CinemaRequest(null, null, 0, DEFAULT_LIMIT, "name,asc")
        );
        Object featuredCinemas = cinemaResult.getOrDefault("data", Collections.emptyList());

        // 4. Thể loại phim (gọi từ MovieService)
        List<Genre> rawGenres = movieService.getGenres();
        List<Map<String, String>> genres = new ArrayList<>();
        if (rawGenres != null) {
            for (Genre g : rawGenres) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("code", g.getGenreCode());
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

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", data);
        result.put("meta", Collections.emptyMap());
        return result;
    }

    private Object extractMovieItems(Map<String, Object> result) {
        if (result == null) {
            return Collections.emptyList();
        }
        Object data = result.get("data");
        if (data instanceof Map<?, ?> dataMap && dataMap.containsKey("items")) {
            return dataMap.get("items");
        }
        return data != null ? data : Collections.emptyList();
    }
}
