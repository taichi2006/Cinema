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

    // Tổng hợp dữ liệu trang chủ (Phim đang chiếu, sắp chiếu, rạp nổi bật, thể loại, thành phố, banner, phim hot, tin tức)
    public Map<String, Object> getHomeData() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 1. Phim đang chiếu & sắp chiếu
        var nowShowingList = movieService.getMovies(new MovieRequest(null, null, "NOW_SHOWING", 0, 10, "releaseDate,desc")).getItems();
        var comingSoonList = movieService.getMovies(new MovieRequest(null, null, "COMING_SOON", 0, 10, "releaseDate,asc")).getItems();

        data.put("nowShowing", nowShowingList);
        data.put("comingSoon", comingSoonList);

        // 2. Phim hot (dùng danh sách phim đang chiếu hoặc nổi bật)
        data.put("hotMovies", nowShowingList);
        data.put("phimHot", nowShowingList);

        // 3. Rạp nổi bật
        data.put("featuredCinemas", cinemaService.getCinemas(new CinemaRequest(null, null, 0, 10, "name,asc")).getItems());

        // 4. Danh sách thể loại
        List<Map<String, String>> genres = new ArrayList<>();
        try {
            for (Genre g : movieService.getGenres()) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("code", String.valueOf(g.getGenreId()));
                item.put("name", g.getGenreName() != null ? g.getGenreName() : "");
                genres.add(item);
            }
        } catch (Exception ignored) {}
        data.put("genres", genres);

        // 5. Danh sách thành phố có rạp
        List<Map<String, String>> cities = new ArrayList<>();
        try {
            for (CityItem c : cinemaService.getCities()) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("code", c.cityCode() != null ? c.cityCode() : "");
                item.put("name", c.cityName() != null ? c.cityName() : "");
                cities.add(item);
            }
        } catch (Exception ignored) {}
        data.put("cities", cities);

        // 6. Dữ liệu Banner
        List<Map<String, Object>> banners = new ArrayList<>();
        for (var m : nowShowingList) {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("id", m.getId());
            b.put("title", m.getTitle());
            b.put("imageUrl", m.getPosterUrl());
            b.put("linkUrl", "/movies/" + m.getId());
            banners.add(b);
            if (banners.size() >= 5) break;
        }
        if (banners.isEmpty()) {
            Map<String, Object> defaultBanner = new LinkedHashMap<>();
            defaultBanner.put("id", "1");
            defaultBanner.put("title", "Khám phá phim chiếu rạp mới nhất");
            defaultBanner.put("imageUrl", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba");
            defaultBanner.put("linkUrl", "/movies");
            banners.add(defaultBanner);
        }
        data.put("banners", banners);
        data.put("banner", banners);

        // 7. Dữ liệu Tin tức / Ưu đãi
        List<Map<String, Object>> news = new ArrayList<>();
        Map<String, Object> newsItem1 = new LinkedHashMap<>();
        newsItem1.put("id", "1");
        newsItem1.put("title", "Ưu đãi thanh toán vé xem phim qua ví điện tử");
        newsItem1.put("summary", "Nhận ngay mã giảm giá khi đặt vé và thanh toán qua ví Cinema");
        newsItem1.put("content", "Chi tiết chương trình khuyến mãi nạp ví và đặt vé xem phim giá ưu đãi.");
        newsItem1.put("imageUrl", "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c");
        news.add(newsItem1);
        data.put("news", news);
        data.put("tinTuc", news);

        return data;
    }
}
