package com.cinema.websocket;

import com.cinema.auth.JwtUtil;
import com.cinema.common.config.ObjectMapperConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.util.List;
import java.util.Map;

@ServerEndpoint(value = "/ws/seats/{showtimeId}")
public class SeatWebSocketEndpoint {

    private final SeatSessionManager sessionManager = SeatSessionManager.getInstance();
    private final ObjectMapper mapper = ObjectMapperConfig.getInstance();

    @OnOpen
    public void onOpen(Session session, @PathParam("showtimeId") String showtimeIdStr) {
        Long showtimeId = parseShowtimeId(showtimeIdStr);
        if (showtimeId == null) {
            closeSession(session, "Mã suất chiếu không hợp lệ");
            return;
        }

        // Lấy token xác thực từ query parameter: ?token=eyJ...
        Long userId = extractUserIdFromSession(session);

        // Tham gia vào phòng của suất chiếu
        sessionManager.joinRoom(showtimeId, session, userId);
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("showtimeId") String showtimeIdStr) {
        Long showtimeId = parseShowtimeId(showtimeIdStr);
        if (showtimeId == null || message == null || message.isBlank()) return;

        try {
            SeatSocketMessage msg = mapper.readValue(message, SeatSocketMessage.class);
            if (msg == null || msg.getType() == null) return;

            // Xử lý gói tin PING - PONG (Keep-Alive)
            if ("PING".equalsIgnoreCase(msg.getType())) {
                sessionManager.sendDirect(session, SeatSocketMessage.pong());
                return;
            }

            Long userId = extractUserIdFromSession(session);
            if (userId == null) {
                sessionManager.sendDirect(session, SeatSocketMessage.error("Vui lòng đăng nhập để thực hiện chọn ghế!"));
                return;
            }

            if ("HOLD_SEAT".equalsIgnoreCase(msg.getType())) {
                if (msg.getSeatId() != null) {
                    sessionManager.holdSeat(showtimeId, msg.getSeatId(), userId, session);
                }
            } else if ("RELEASE_SEAT".equalsIgnoreCase(msg.getType())) {
                if (msg.getSeatId() != null) {
                    sessionManager.releaseSeat(showtimeId, msg.getSeatId(), userId);
                }
            }
        } catch (Exception e) {
            sessionManager.sendDirect(session, SeatSocketMessage.error("Dữ liệu socket không hợp lệ"));
        }
    }

    @OnClose
    public void onClose(Session session) {
        sessionManager.leaveRoom(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        sessionManager.leaveRoom(session);
    }

    private Long parseShowtimeId(String str) {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            return null;
        }
    }

    private Long extractUserIdFromSession(Session session) {
        try {
            Map<String, List<String>> params = session.getRequestParameterMap();
            List<String> tokenList = params.get("token");
            if (tokenList != null && !tokenList.isEmpty()) {
                String token = tokenList.get(0);
                if (token != null && !token.isBlank()) {
                    Claims claims = JwtUtil.parseAccessToken(token);
                    return Long.parseLong(claims.getSubject());
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void closeSession(Session session, String reason) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, reason));
        } catch (Exception ignored) {}
    }
}
