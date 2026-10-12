package com.cinema.websocket;

import com.cinema.common.config.ObjectMapperConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SeatSessionManager {

    private static final SeatSessionManager INSTANCE = new SeatSessionManager();
    private final ObjectMapper mapper = ObjectMapperConfig.getInstance();

    // 1. Quản lý các Session trong phòng chiếu: showtimeId -> Set<Session>
    private final Map<Long, Set<Session>> showtimeRooms = new ConcurrentHashMap<>();

    // 2. Quản lý trạng thái ghế đang giữ tạm thời: showtimeId -> (seatId -> userId)
    private final Map<Long, Map<Long, Long>> showtimeHeldSeats = new ConcurrentHashMap<>();

    // 3. Mapping session -> userId và session -> showtimeId để dọn dẹp khi disconnect
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionShowtimeMap = new ConcurrentHashMap<>();

    private SeatSessionManager() {}

    public static SeatSessionManager getInstance() {
        return INSTANCE;
    }

    // Client tham gia phòng theo dõi suất chiếu
    public void joinRoom(Long showtimeId, Session session, Long userId) {
        if (showtimeId == null || session == null) return;

        showtimeRooms.computeIfAbsent(showtimeId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionShowtimeMap.put(session.getId(), showtimeId);
        if (userId != null) {
            sessionUserMap.put(session.getId(), userId);
        }

        // Gửi ngay trạng thái các ghế đang bị giữ cho người mới vào phòng
        Map<Long, Long> heldSeats = showtimeHeldSeats.getOrDefault(showtimeId, Collections.emptyMap());
        sendDirect(session, SeatSocketMessage.initialState(showtimeId, new HashMap<>(heldSeats)));
    }

    // Client rời phòng hoặc mất mạng
    public void leaveRoom(Session session) {
        if (session == null) return;

        String sessionId = session.getId();
        Long showtimeId = sessionShowtimeMap.remove(sessionId);
        Long userId = sessionUserMap.remove(sessionId);

        if (showtimeId != null) {
            Set<Session> room = showtimeRooms.get(showtimeId);
            if (room != null) {
                room.remove(session);
                if (room.isEmpty()) {
                    showtimeRooms.remove(showtimeId);
                }
            }

            // Tự động nhả các ghế mà user này đang giữ tạm thời (chưa tạo đơn) khi họ tắt tab
            if (userId != null) {
                Map<Long, Long> heldSeats = showtimeHeldSeats.get(showtimeId);
                if (heldSeats != null) {
                    List<Long> seatsToRelease = new ArrayList<>();
                    heldSeats.forEach((seatId, heldUserId) -> {
                        if (Objects.equals(heldUserId, userId)) {
                            seatsToRelease.add(seatId);
                        }
                    });

                    for (Long sId : seatsToRelease) {
                        heldSeats.remove(sId);
                        broadcast(showtimeId, SeatSocketMessage.holdChanged(showtimeId, sId, userId, "AVAILABLE"));
                    }
                }
            }
        }
    }

    // Client click chọn giữ ghế
    public boolean holdSeat(Long showtimeId, Long seatId, Long userId, Session session) {
        if (showtimeId == null || seatId == null || userId == null) return false;

        Map<Long, Long> heldSeats = showtimeHeldSeats.computeIfAbsent(showtimeId, k -> new ConcurrentHashMap<>());

        // Kiểm tra xem ghế đã bị ai khác giữ chưa
        Long currentHolder = heldSeats.get(seatId);
        if (currentHolder != null && !Objects.equals(currentHolder, userId)) {
            // Ghế đã có người khác nhanh tay giữ trước -> báo lỗi riêng cho session này
            sendDirect(session, SeatSocketMessage.error("Ghế đã có người khác chọn trước!"));
            return false;
        }

        // Khóa ghế cho user này
        heldSeats.put(seatId, userId);

        // Phát sóng cho TẤT CẢ mọi người trong phòng: Ghế này chuyển sang trạng thái HOLDING
        broadcast(showtimeId, SeatSocketMessage.holdChanged(showtimeId, seatId, userId, "HOLDING"));
        return true;
    }

    // Client click bỏ chọn ghế
    public boolean releaseSeat(Long showtimeId, Long seatId, Long userId) {
        if (showtimeId == null || seatId == null || userId == null) return false;

        Map<Long, Long> heldSeats = showtimeHeldSeats.get(showtimeId);
        if (heldSeats != null) {
            Long currentHolder = heldSeats.get(seatId);
            // Chỉ người đang giữ ghế mới có quyền nhả ghế
            if (Objects.equals(currentHolder, userId)) {
                heldSeats.remove(seatId);
                // Phát sóng: Ghế này quay lại trạng thái AVAILABLE
                broadcast(showtimeId, SeatSocketMessage.holdChanged(showtimeId, seatId, userId, "AVAILABLE"));
                return true;
            }
        }
        return false;
    }

    // Khi thanh toán xong đơn vé -> đánh dấu ghế đã bán
    public void markBooked(Long showtimeId, List<Long> seatIds) {
        if (showtimeId == null || seatIds == null || seatIds.isEmpty()) return;

        Map<Long, Long> heldSeats = showtimeHeldSeats.get(showtimeId);
        for (Long seatId : seatIds) {
            if (heldSeats != null) {
                heldSeats.remove(seatId);
            }
            // Phát sóng: Ghế đã bán thành công BOOKED
            broadcast(showtimeId, SeatSocketMessage.holdChanged(showtimeId, seatId, null, "BOOKED"));
        }
    }

    // Phát sóng tin nhắn cho tất cả các client đang xem cùng 1 suất chiếu
    public void broadcast(Long showtimeId, SeatSocketMessage message) {
        Set<Session> room = showtimeRooms.get(showtimeId);
        if (room == null || room.isEmpty()) return;

        String jsonText;
        try {
            jsonText = mapper.writeValueAsString(message);
        } catch (Exception e) {
            return;
        }

        for (Session session : room) {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(jsonText);
            }
        }
    }

    // Gửi tin nhắn trực tiếp cho 1 session
    public void sendDirect(Session session, SeatSocketMessage message) {
        if (session != null && session.isOpen()) {
            try {
                String jsonText = mapper.writeValueAsString(message);
                session.getAsyncRemote().sendText(jsonText);
            } catch (Exception ignored) {}
        }
    }
}
