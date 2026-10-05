package com.cinema.cinema;

import com.cinema.cinema.DTO.Request.CinemaRequest;
import com.cinema.cinema.DTO.Response.CinemaResponse;
import com.cinema.cinema.DTO.Response.CinemaShowtimeResponse;
import com.cinema.common.dto.CommonDTO.PageMeta;
import com.cinema.common.exception.ApiException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        PageMeta meta = new PageMeta(request.getPage(), request.getSize(), totalElements, totalPages);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", cinemaResponses);
        result.put("meta", meta);
        return result;
    }

    public Map<String, Object> getCinemaById(String idStr) {
        Long cinemaId = parseAndValidateCinemaId(idStr);
        Cinema cinema = cinemaDAO.findById(cinemaId)
                .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy rạp"));

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

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", response);
        result.put("meta", Collections.emptyMap());
        return result;
    }

    public Map<String, Object> getCinemaShowtimes(String idStr, String dateStr, String movieIdStr, int page, int size) {
        Long cinemaId = parseAndValidateCinemaId(idStr);

        cinemaDAO.findById(cinemaId)
                .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy rạp"));

        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw ApiException.badRequest("Tham số 'date' là bắt buộc (định dạng YYYY-MM-DD).");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("Tham số 'date' không hợp lệ hoặc sai định dạng YYYY-MM-DD: " + dateStr);
        }

        Long movieId = null;
        if (movieIdStr != null && !movieIdStr.trim().isEmpty()) {
            try {
                movieId = Long.parseLong(movieIdStr.trim());
                if (movieId <= 0) {
                    throw ApiException.badRequest("Tham số 'movieId' phải là số nguyên dương.");
                }
            } catch (NumberFormatException e) {
                throw ApiException.badRequest("Tham số 'movieId' không hợp lệ: " + movieIdStr);
            }
        }

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
        int totalPages = size > 0
                ? (int) Math.ceil((double) totalElements / size)
                : 0;

        PageMeta meta = new PageMeta(page, size, totalElements, totalPages);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", showtimes);
        result.put("meta", meta);
        return result;
    }

    private Long parseAndValidateCinemaId(String idStr) {
        if (idStr == null || idStr.trim().isEmpty()) {
            throw ApiException.badRequest("Mã rạp 'id' không được để trống.");
        }
        try {
            long id = Long.parseLong(idStr.trim());
            if (id <= 0) {
                throw ApiException.badRequest("Mã rạp 'id' phải là số nguyên dương.");
            }
            return id;
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Mã rạp 'id' không hợp lệ: " + idStr);
        }
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
}
