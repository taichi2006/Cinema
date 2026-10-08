package com.cinema.showtime.service;

import com.cinema.common.exception.ApiException;
import com.cinema.showtime.dao.ShowtimeDAO;
import com.cinema.showtime.dto.response.SeatMapResponse;
import com.cinema.showtime.dto.response.SeatResponse;
import com.cinema.showtime.dto.response.ShowtimeResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShowtimeService {

    private final ShowtimeDAO showtimeDAO;

    public ShowtimeService() {
        this.showtimeDAO = new ShowtimeDAO();
    }

    public ShowtimeService(ShowtimeDAO showtimeDAO) {
        this.showtimeDAO = showtimeDAO;
    }

    public Map<String, Object> getShowtimeDetail(String idStr) {
        Long showtimeId = parseAndValidateShowtimeId(idStr);

        ShowtimeResponse showtime = showtimeDAO.findShowtimeById(showtimeId)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay suat chieu"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", showtime);
        return result;
    }

    public Map<String, Object> getSeatMap(String idStr, Long currentUserId) {
        Long showtimeId = parseAndValidateShowtimeId(idStr);

        if (showtimeDAO.findShowtimeById(showtimeId).isEmpty() && showtimeDAO.findShowtimeInfo(showtimeId).isEmpty()) {
            throw ApiException.notFound("Khong tim thay suat chieu");
        }

        List<SeatResponse> seats = showtimeDAO.findSeatsByShowtimeId(showtimeId);

        SeatMapResponse seatMap = new SeatMapResponse(showtimeId, seats);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", seatMap);
        return result;
    }

    private Long parseAndValidateShowtimeId(String idStr) {
        if (idStr == null || idStr.trim().isEmpty()) {
            throw ApiException.badRequest("Mã suất chiếu 'id' không được để trống.");
        }
        try {
            long id = Long.parseLong(idStr.trim());
            if (id <= 0) {
                throw ApiException.badRequest("Mã suất chiếu 'id' phải là số nguyên dương.");
            }
            return id;
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Mã suất chiếu 'id' không hợp lệ: " + idStr);
        }
    }
}
