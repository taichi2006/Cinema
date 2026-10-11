package com.cinema.cinema;

import com.cinema.cinema.dto.request.CinemaRequest;
import com.cinema.cinema.dto.response.CinemaResponse;
import com.cinema.cinema.dto.response.CinemaShowtimeResponse;
import com.cinema.common.dto.PageMeta;
import com.cinema.common.dto.PageResponse;
import com.cinema.common.exception.ApiException;

import java.time.LocalDate;
import java.util.List;

public class CinemaService {

    private final CinemaDAO cinemaDAO = new CinemaDAO();

    // 1. Danh sách rạp chiếu (tìm kiếm, lọc theo thành phố, phân trang)
    public PageResponse<CinemaResponse> getCinemas(CinemaRequest req) {
        List<CinemaResponse> items = cinemaDAO.findCinemas(req).stream()
                .map(this::toResponse)
                .toList();
        long total = cinemaDAO.countCinemas(req);
        return PageResponse.of(items, new PageMeta(req.getPage(), req.getSize(), total));
    }

    // 2. Chi tiết rạp
    public CinemaResponse getCinemaById(long cinemaId) {
        return cinemaDAO.findById(cinemaId)
                .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()))
                .map(this::toResponse)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy rạp"));
    }

    // 3. Suất chiếu theo rạp
    public PageResponse<CinemaShowtimeResponse> getCinemaShowtimes(long cinemaId, LocalDate date, Long movieId, int page, int size) {
        cinemaDAO.findById(cinemaId)
                .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy rạp"));

        List<CinemaShowtimeResponse> showtimes = cinemaDAO.findShowtimes(cinemaId, date, movieId, page, size);
        long total = cinemaDAO.countShowtimes(cinemaId, date, movieId);
        return PageResponse.of(showtimes, new PageMeta(page, size, total));
    }

    // 4. Danh sách các thành phố có rạp
    public List<CinemaDAO.CityItem> getCities() {
        return cinemaDAO.findDistinctCities();
    }

    private CinemaResponse toResponse(Cinema c) {
        return new CinemaResponse(
                String.valueOf(c.getCinemaId()),
                c.getCinemaName(),
                c.getCityCode(),
                c.getCityName(),
                c.getAddress(),
                c.getPhone(),
                c.getImageUrl(),
                c.getLatitude(),
                c.getLongitude()
        );
    }
}
