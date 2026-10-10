import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

import com.cinema.auth.AuthService;
import com.cinema.auth.dto.request.ChangePasswordRequest;
import com.cinema.common.dto.ApiResponse;
import com.cinema.common.exception.ApiException;
import com.cinema.common.web.BaseServlet;
import com.cinema.user.criteria.UserBookingCriteria;
import com.cinema.user.criteria.UserVoucherCriteria;
import com.cinema.user.dto.request.UpdateUserRequest;
import com.cinema.user.service.UserBookingService;
import com.cinema.user.service.UserService;
import com.cinema.user.service.UserVoucherService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/user", "/user/*" })
public class UserController extends BaseServlet {

    private final UserService service = new UserService();
    private final UserBookingService bookingService = new UserBookingService();
    private final UserVoucherService voucherService = new UserVoucherService();
    private final AuthService authService = new AuthService();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(req.getMethod())) {
            doPatch(req, resp);
            return;
        }
        super.service(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = getAuthenticatedUserId(req);
        String[] segments = getPathSegments(req);
        if (segments.length == 0) {
            writeSuccess(resp, service.getProfile(userId));
        } else if (segments.length == 1) {
            String segment = segments[0];
            if ("bookings".equals(segment) || "booking".equals(segment)) {
                handleGetBookings(req, resp, userId);
            } else if ("vouchers".equals(segment) || "voucher".equals(segment)) {
                handleGetVouchers(req, resp, userId);
            } else {
                throw ApiException.notFound("Endpoint không tồn tại");
            }
        } else {
            throw ApiException.notFound("Endpoint không tồn tại");
        }
    }

    private void handleGetBookings(HttpServletRequest req, HttpServletResponse resp, long userId) throws IOException {
        String status = req.getParameter("status");
        if (status != null && !status.isBlank()) {
            status = status.trim().toUpperCase();
            if (!Set.of("PENDING", "SUCCESS", "FAILED", "CANCELLED").contains(status)) {
                throw ApiException.badRequest("Trạng thái booking không hợp lệ");
            }
        } else {
            status = null;
        }

        LocalDate from = parseDateParam(req, "from");
        LocalDate to = parseDateParam(req, "to");
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest("from không được lớn hơn to");
        }

        int page = getIntParam(req, "page", 0);
        int size = getIntParam(req, "size", 20);
        if (page < 0) throw ApiException.badRequest("page phải lớn hơn hoặc bằng 0");
        if (size < 1 || size > 100) throw ApiException.badRequest("size phải từ 1 đến 100");

        String sort = req.getParameter("sort");
        UserBookingCriteria.SortDirection direction = UserBookingCriteria.SortDirection.DESC;
        if (sort != null) {
            if (sort.equalsIgnoreCase("createdAt,asc") || sort.equalsIgnoreCase("asc")) {
                direction = UserBookingCriteria.SortDirection.ASC;
            }
        }

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        Instant fromInstant = from == null ? null : from.atStartOfDay(zone).toInstant();
        Instant toInstant = to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant();

        var criteria = new UserBookingCriteria(status, fromInstant, toInstant, page, size, direction);
        writeSuccess(resp, bookingService.getBookingHistory(userId, criteria));
    }

    private void handleGetVouchers(HttpServletRequest req, HttpServletResponse resp, long userId) throws IOException {
        String status = req.getParameter("status");
        if (status != null && !status.isBlank()) {
            status = status.trim().toUpperCase();
            if ("AVAILABLE".equals(status)) status = "UNUSED";
            if (!Set.of("UNUSED", "USED", "EXPIRED").contains(status)) {
                throw ApiException.badRequest("Trạng thái voucher không hợp lệ");
            }
        } else {
            status = null;
        }

        int page = getIntParam(req, "page", 0);
        int size = getIntParam(req, "size", 20);
        if (page < 0) throw ApiException.badRequest("page phải lớn hơn hoặc bằng 0");
        if (size < 1 || size > 100) throw ApiException.badRequest("size phải từ 1 đến 100");

        var criteria = new UserVoucherCriteria(status, page, size);
        writeSuccess(resp, voucherService.getUserVouchers(userId, criteria));
    }

    private LocalDate parseDateParam(HttpServletRequest req, String param) {
        String val = req.getParameter(param);
        if (val == null || val.isBlank()) return null;
        try {
            return LocalDate.parse(val.trim());
        } catch (DateTimeException e) {
            throw ApiException.badRequest(param + " phải có định dạng yyyy-MM-dd");
        }
    }

    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = getAuthenticatedUserId(req);
        String[] segments = getPathSegments(req);
        if (segments.length == 0) {
            var body = parseBody(req, UpdateUserRequest.class);
            writeSuccess(resp, service.updateProfile(userId, body));
        } else {
            throw ApiException.notFound("Endpoint không tồn tại");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = getAuthenticatedUserId(req);
        String[] segments = getPathSegments(req);
        if (segments.length != 1 || !"change-password".equals(segments[0])) {
            throw ApiException.notFound("Endpoint không tồn tại");
        }

        var body = parseBody(req, ChangePasswordRequest.class);
        authService.changePassword(userId, body);
        writeSuccess(resp, null);
    }
}
