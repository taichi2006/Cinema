package com.cinema.cinema.service;

import com.cinema.cinema.dao.CinemaDAO;
import com.cinema.cinema.dao.CinemaDAO.CityItem;
import com.cinema.cinema.dto.request.CinemaRequest;
import com.cinema.cinema.dto.response.CinemaResponse;
import com.cinema.cinema.entity.Cinema;
import com.cinema.common.dto.CommonDTO.PageMeta;
import com.cinema.common.exception.ApiException;
import com.cinema.movie.dto.response.ShowtimeResponse;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CinemaService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "INACTIVE", "MAINTENANCE");

    private final CinemaDAO cinemaDAO;

    public CinemaService() {
        this.cinemaDAO = new CinemaDAO();
    }

    public CinemaService(CinemaDAO cinemaDAO) {
        this.cinemaDAO = cinemaDAO;
    }

    public Map<String, Object> getCinemas(CinemaRequest request) {
        validateRequest(request);

        List<Cinema> cinemas = cinemaDAO.findCinemas(request);
        long totalElements = cinemaDAO.countCinemas(request);
        int totalPages = request.getSize() > 0
                ? (int) Math.ceil((double) totalElements / request.getSize())
                : 0;

        List<CinemaResponse> cinemaResponses = new ArrayList<>();
        for (Cinema cinema : cinemas) {
            CinemaResponse response = new CinemaResponse(
                    cinema.getCinemaId(),
                    cinema.getCinemaName(),
                    cinema.getAddress(),
                    cinema.getCity(),
                    cinema.getPhone(),
                    cinema.getEmail(),
                    cinema.getStatus()
            );
            cinemaResponses.add(response);
        }

        PageMeta meta = new PageMeta(request.getPage(), request.getSize(), totalElements, totalPages);
        Map<String, Object> pageData = new LinkedHashMap<>();
        pageData.put("items", cinemaResponses);
        pageData.put("meta", meta);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", pageData);
        return result;
    }

    public Map<String, Object> getCinemaById(String idStr) {
        Long cinemaId = parseAndValidateCinemaId(idStr);
        Cinema cinema = cinemaDAO.findById(cinemaId)
                .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()))
                .orElseThrow(() -> ApiException.notFound("Khong tim thay rap"));

        CinemaResponse response = new CinemaResponse(
                cinema.getCinemaId(),
                cinema.getCinemaName(),
                cinema.getAddress(),
                cinema.getCity(),
                cinema.getPhone(),
                cinema.getEmail(),
                cinema.getStatus()
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", response);
        return result;
    }

    public Map<String, Object> getCinemaShowtimes(String idStr, String dateStr, String movieIdStr) {
        Long cinemaId = parseAndValidateCinemaId(idStr);

        cinemaDAO.findById(cinemaId)
                .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()))
                .orElseThrow(() -> ApiException.notFound("Khong tim thay rap"));

        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw ApiException.badRequest("Tham so date la bat buoc");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("Định dạng ngày 'date' không hợp lệ (yêu cầu: YYYY-MM-DD): " + dateStr);
        }

        Long movieId = null;
        if (movieIdStr != null && !movieIdStr.trim().isEmpty()) {
            try {
                movieId = Long.parseLong(movieIdStr.trim());
                if (movieId <= 0) {
                    throw ApiException.badRequest("Mã phim 'movieId' phải là số nguyên dương.");
                }
            } catch (NumberFormatException e) {
                throw ApiException.badRequest("Mã phim 'movieId' không hợp lệ: " + movieIdStr);
            }
        }

        List<ShowtimeResponse> showtimes = cinemaDAO.findShowtimes(cinemaId, date, movieId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", showtimes);
        return result;
    }

    // Overload hỗ trợ code cũ nếu có
    public Map<String, Object> getCinemaShowtimes(String idStr, String dateStr, String movieIdStr, int page, int size) {
        return getCinemaShowtimes(idStr, dateStr, movieIdStr);
    }

    private Long parseAndValidateCinemaId(String idStr) {
        if (idStr == null || idStr.trim().isEmpty()) {
            throw ApiException.badRequest("Mã rạp 'cinemaId' không được để trống.");
        }
        try {
            long id = Long.parseLong(idStr.trim());
            if (id <= 0) {
                throw ApiException.badRequest("Mã rạp 'cinemaId' phải là số nguyên dương.");
            }
            return id;
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Mã rạp 'cinemaId' không hợp lệ: " + idStr);
        }
    }

    private void validateRequest(CinemaRequest request) {
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            String upperStatus = request.getStatus().trim().toUpperCase();
            if (!ALLOWED_STATUSES.contains(upperStatus)) {
                throw ApiException.badRequest(
                        "Trạng thái rạp 'status' không hợp lệ: " + request.getStatus() +
                                ". Các giá trị hợp lệ: " + ALLOWED_STATUSES
                );
            }
            request.setStatus(upperStatus);
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

    public List<CityItem> getCities() {
        return cinemaDAO.findDistinctCities();
    }
}
