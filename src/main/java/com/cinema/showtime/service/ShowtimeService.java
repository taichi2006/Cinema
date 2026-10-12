package com.cinema.showtime.service;

import com.cinema.common.exception.ApiException;
import com.cinema.showtime.dao.ShowtimeDAO;
import com.cinema.showtime.dto.response.SeatMapResponse;
import com.cinema.showtime.dto.response.SeatResponse;
import com.cinema.showtime.dto.response.ShowtimeDetailResponse;

import java.time.Instant;
import java.util.List;

public class ShowtimeService {

    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();

    // 1. Chi tiết suất chiếu
    public ShowtimeDetailResponse getShowtimeDetail(long showtimeId) {
        ShowtimeDAO.ShowtimeInfo info = showtimeDAO.findShowtimeInfo(showtimeId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy suất chiếu"));
        return toDetailResponse(info);
    }

    // 2. Sơ đồ ghế và trạng thái giữ chỗ
    public SeatMapResponse getSeatMap(long showtimeId, Long currentUserId) {
        ShowtimeDAO.ShowtimeInfo info = showtimeDAO.findShowtimeInfo(showtimeId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy suất chiếu"));

        if (!"SCHEDULED".equalsIgnoreCase(info.status()) && !"ONGOING".equalsIgnoreCase(info.status())) {
            throw new ApiException(422, "Suất chiếu đã bắt đầu hoặc đã đóng bán");
        }
        if (info.startsAtInstant() != null && info.startsAtInstant().isBefore(Instant.now())) {
            throw new ApiException(422, "Suất chiếu đã bắt đầu hoặc đã đóng bán");
        }

        List<SeatResponse> seats = showtimeDAO.findSeatsForShowtime(showtimeId, info.roomId(), currentUserId);
        String screenPosition = info.screenPosition() != null ? info.screenPosition() : "TOP";

        return new SeatMapResponse(
                toDetailResponse(info),
                Instant.now().toString(),
                screenPosition,
                seats
        );
    }

    private ShowtimeDetailResponse toDetailResponse(ShowtimeDAO.ShowtimeInfo info) {
        return new ShowtimeDetailResponse(
                String.valueOf(info.showtimeId()),
                String.valueOf(info.movieId()),
                info.movieTitle(),
                String.valueOf(info.cinemaId()),
                info.cinemaName(),
                String.valueOf(info.roomId()),
                info.roomName(),
                info.startsAt(),
                info.endsAt(),
                info.format(),
                info.language(),
                info.minTicketPrice(),
                info.currency(),
                info.availableSeatCount()
        );
    }
}
