package com.cinema.showtime;

import com.cinema.common.exception.ApiException;
import com.cinema.showtime.DTO.Response.SeatMapResponse;
import com.cinema.showtime.DTO.Response.SeatResponse;
import com.cinema.showtime.DTO.Response.ShowtimeDetailResponse;

import java.time.Instant;
import java.util.Collections;
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

    public Map<String, Object> getSeatMap(String idStr, Long currentUserId) {
        Long showtimeId = parseAndValidateShowtimeId(idStr);

        ShowtimeDAO.ShowtimeInfo showtimeInfo = showtimeDAO.findShowtimeInfo(showtimeId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy suất chiếu"));

        if (!"OPEN".equalsIgnoreCase(showtimeInfo.status())) {
            throw new ApiException(422, "Suất chiếu đã bắt đầu hoặc đã đóng bán");
        }

        if (showtimeInfo.startsAtInstant() != null && showtimeInfo.startsAtInstant().isBefore(Instant.now())) {
            throw new ApiException(422, "Suất chiếu đã bắt đầu hoặc đã đóng bán");
        }

        List<SeatResponse> seats = showtimeDAO.findSeatsForShowtime(
                showtimeId,
                showtimeInfo.roomId(),
                currentUserId
        );

        ShowtimeDetailResponse showtimeDetail = new ShowtimeDetailResponse(
                String.valueOf(showtimeInfo.showtimeId()),
                showtimeInfo.movieId() != null ? String.valueOf(showtimeInfo.movieId()) : null,
                showtimeInfo.movieTitle(),
                showtimeInfo.cinemaId() != null ? String.valueOf(showtimeInfo.cinemaId()) : null,
                showtimeInfo.cinemaName(),
                showtimeInfo.roomId() != null ? String.valueOf(showtimeInfo.roomId()) : null,
                showtimeInfo.roomName(),
                showtimeInfo.startsAt(),
                showtimeInfo.endsAt(),
                showtimeInfo.format(),
                showtimeInfo.language(),
                showtimeInfo.minTicketPrice(),
                showtimeInfo.currency(),
                showtimeInfo.availableSeatCount()
        );

        String serverTime = Instant.now().toString();
        String screenPosition = showtimeInfo.screenPosition() != null ? showtimeInfo.screenPosition() : "TOP";

        SeatMapResponse seatMap = new SeatMapResponse(
                showtimeDetail,
                serverTime,
                screenPosition,
                seats
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", seatMap);
        result.put("meta", Collections.emptyMap());
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
