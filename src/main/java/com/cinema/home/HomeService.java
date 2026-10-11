package com.cinema.home;

import com.cinema.cinema.CinemaDAO.CityItem;
import com.cinema.cinema.CinemaService;
import com.cinema.cinema.dto.request.CinemaRequest;
import com.cinema.movie.dto.request.MovieRequest;
import com.cinema.movie.Genre;
import com.cinema.movie.MovieService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HomeService {

    private final MovieService movieService = new MovieService();
    private final CinemaService cinemaService = new CinemaService();

    // Tổng hợp dữ liệu trang chủ (Phim đang chiếu, sắp chiếu, rạp nổi bật, thể loại, thành phố)
    public Map<String, Object> getHomeData() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 1. Phim đang chiếu & sắp chiếu
        data.put("nowShowing", movieService.getMovies(new MovieRequest(null, null, "NOW_SHOWING", 0, 10, "releaseDate,desc")).getItems());
        data.put("comingSoon", movieService.getMovies(new MovieRequest(null, null, "COMING_SOON", 0, 10, "releaseDate,asc")).getItems());

        // 2. Rạp nổi bật
        data.put("featuredCinemas", cinemaService.getCinemas(new CinemaRequest(null, null, 0, 10, "name,asc")).getItems());

        // 3. Danh sách thể loại
        List<Map<String, String>> genres = new ArrayList<>();
        for (Genre g : movieService.getGenres()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("code", String.valueOf(g.getGenreId()));
            item.put("name", g.getGenreName() != null ? g.getGenreName() : "");
            genres.add(item);
        }
        data.put("genres", genres);

        // 4. Danh sách thành phố có rạp
        List<Map<String, String>> cities = new ArrayList<>();
        for (CityItem c : cinemaService.getCities()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("code", c.cityCode() != null ? c.cityCode() : "");
            item.put("name", c.cityName() != null ? c.cityName() : "");
            cities.add(item);
        }
        data.put("cities", cities);

        return data;
    }
}
