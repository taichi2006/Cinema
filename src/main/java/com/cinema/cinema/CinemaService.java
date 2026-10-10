package com.cinema.cinema;

import com.cinema.cinema.DTO.Request.CinemaRequest;
import com.cinema.cinema.DTO.Response.CinemaResponse;
import com.cinema.cinema.DTO.Response.CinemaShowtimeResponse;
import com.cinema.common.dto.PageMeta;
import com.cinema.common.dto.PageResponse;
import com.cinema.common.exception.ApiException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CinemaService {

    private static final Set<String> ALLOWED_SORTS = Set.of("name,asc", "name,desc");

    private final CinemaDAO cinemaDAO;

    public CinemaService() {
        this.cinemaDAO = new CinemaDAO();
    }

    public CinemaService(CinemaDAO cinemaDAO) {
        this.cinemaDAO = cinemaDAO;
    }

    public PageResponse<CinemaResponse> getCinemas(CinemaRequest request) {
        validateRequest(request);

        List<Cinema> cinemas = cinemaDAO.findCinemas(request);
        long totalElements = cinemaDAO.countCinemas(request);

        List<CinemaResponse> cinemaResponses = new ArrayList<>();
        for (Cinema cinema : cinemas) {
            CinemaResponse response = new CinemaResponse(
                    String.valueOf(cinema.getCinemaId()),
                    cinema.getCinemaName(),
                    cinema.getCityCode(),
                    cinema.getCityName(),
                    cinema.getAddress(),
                    cinema.getPhone(),
                    cinema.getImageUrl(),
                    cinema.getLatitude(),
                    cinema.getLongitude()
            );
            cinemaResponses.add(response);
        }

        PageMeta meta = new PageMeta(request.getPage(), request.getSize(), totalElements);
        return PageResponse.of(cinemaResponses, meta);
    }

    public CinemaResponse getCinemaById(long cinemaId) {
        Cinema cinema = cinemaDAO.findById(cinemaId)
                .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy rạp"));

        return new CinemaResponse(
                String.valueOf(cinema.getCinemaId()),
                cinema.getCinemaName(),
                cinema.getCityCode(),
                cinema.getCityName(),
                cinema.getAddress(),
                cinema.getPhone(),
                cinema.getImageUrl(),
                cinema.getLatitude(),
                cinema.getLongitude()
        );
    }

    public PageResponse<CinemaShowtimeResponse> getCinemaShowtimes(long cinemaId, LocalDate date, Long movieId, int page, int size) {
        cinemaDAO.findById(cinemaId)
                .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy rạp"));

        if (page < 0) {
            throw ApiException.badRequest("Số trang 'page' phải lớn hơn hoặc bằng 0.");
        }

        if (size < 1 || size > 50) {
            throw ApiException.badRequest("Kích thước trang 'size' phải nằm trong khoảng từ 1 đến 50.");
        }

        long offset = (long) page * size;
        if (offset > Integer.MAX_VALUE || offset < 0) {
            throw ApiException.badRequest("Vị trí phân trang vượt quá giới hạn cho phép.");
        }

        List<CinemaShowtimeResponse> showtimes = cinemaDAO.findShowtimes(cinemaId, date, movieId, page, size);
        long totalElements = cinemaDAO.countShowtimes(cinemaId, date, movieId);

        PageMeta meta = new PageMeta(page, size, totalElements);
        return PageResponse.of(showtimes, meta);
    }

    private void validateRequest(CinemaRequest request) {
        if (request.getQ() != null && request.getQ().trim().length() > 100) {
            throw ApiException.badRequest("Từ khóa tìm kiếm 'q' không được vượt quá 100 ký tự.");
        }

        if (request.getSort() != null && !request.getSort().trim().isEmpty()) {
            if (!ALLOWED_SORTS.contains(request.getSort().trim())) {
                throw ApiException.badRequest(
                        "Tham số 'sort' không hợp lệ: " + request.getSort() +
                                ". Các giá trị hợp lệ: [name,asc, name,desc]"
                );
            }
        } else {
            request.setSort("name,asc");
        }

        if (request.getPage() < 0) {
            throw ApiException.badRequest("Số trang 'page' phải lớn hơn hoặc bằng 0.");
        }

        if (request.getSize() < 1 || request.getSize() > 50) {
            throw ApiException.badRequest("Kích thước trang 'size' phải nằm trong khoảng từ 1 đến 50.");
        }

        long offset = (long) request.getPage() * request.getSize();
        if (offset > Integer.MAX_VALUE || offset < 0) {
            throw ApiException.badRequest("Vị trí phân trang vượt quá giới hạn cho phép.");
        }
    }

    public List<CinemaDAO.CityItem> getCities() {
        return cinemaDAO.findDistinctCities();
    }
}
