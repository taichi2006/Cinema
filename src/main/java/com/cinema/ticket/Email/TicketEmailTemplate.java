package com.cinema.ticket.Email;

import com.cinema.ticket.Ticket;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Trình tạo mẫu giao diện Email HTML chuyên nghiệp, hiển thị thông tin vé xem
 * phim điện tử.
 */
public class TicketEmailTemplate {

    private static final Locale VI_LOCALE = Locale.of("vi", "VN");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getInstance(VI_LOCALE);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm - EEEE, dd/MM/yyyy",
            VI_LOCALE);

    public static String buildTicketEmailHtml(
            String recipientName,
            String movieTitle,
            String cinemaName,
            String roomName,
            LocalDateTime showTime,
            List<Ticket> tickets,
            BigDecimal totalAmount) {

        String formattedTime = (showTime != null)
                ? showTime.format(DATE_TIME_FORMATTER)
                : "Theo thông tin trên vé";

        String formattedTotal = (totalAmount != null)
                ? CURRENCY_FORMAT.format(totalAmount) + " ₫"
                : "Đã thanh toán";

        StringBuilder ticketRows = new StringBuilder();
        if (tickets != null && !tickets.isEmpty()) {
            int index = 1;
            for (Ticket t : tickets) {
                String seatName = t.getSeatName() != null ? t.getSeatName() : "-";
                String ticketCode = t.getTicketCode() != null ? t.getTicketCode() : "TKT-" + t.getTicketId();
                String priceStr = t.getPrice() != null ? CURRENCY_FORMAT.format(t.getPrice()) + " ₫" : "-";

                ticketRows.append("""
                            <tr style="border-bottom: 1px solid #e5e7eb;">
                                <td style="padding: 10px 8px; color: #6b7280; font-size: 13px; text-align: center;">%d</td>
                                <td style="padding: 10px 8px; color: #111827; font-weight: 600; font-size: 14px; text-align: center;">%s</td>
                                <td style="padding: 10px 8px; text-align: center;">
                                    <span style="font-family: monospace; font-size: 13px; font-weight: 600; color: #1f2937; background-color: #f3f4f6; padding: 3px 8px; border-radius: 4px; letter-spacing: 0.5px;">%s</span>
                                </td>
                                <td style="padding: 10px 8px; color: #111827; font-size: 14px; text-align: right;">%s</td>
                            </tr>
                        """.formatted(index++, seatName, ticketCode, priceStr));
            }
        } else {
            ticketRows.append("""
                        <tr>
                            <td colspan="4" style="padding: 14px; text-align: center; color: #6b7280; font-size: 13px;">Thông tin vé đang được cập nhật</td>
                        </tr>
                    """);
        }

        String safeCustomerName = (recipientName != null && !recipientName.isBlank()) ? recipientName : "Quý khách";

        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Xác nhận vé xem phim</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f4f5f7; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #374151; -webkit-font-smoothing: antialiased;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color: #f4f5f7; padding: 30px 12px;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="100%%" style="max-width: 580px; background-color: #ffffff; border-radius: 8px; border: 1px solid #e5e7eb; overflow: hidden;">
                                    <!-- Header -->
                                    <tr>
                                        <td style="padding: 28px 28px 20px; border-bottom: 1px solid #f3f4f6;">
                                            <div style="font-size: 12px; font-weight: 700; color: #6b7280; letter-spacing: 1px; text-transform: uppercase;">CINEMA BOOKING</div>
                                            <h1 style="margin: 8px 0 4px; font-size: 20px; font-weight: 700; color: #111827;">Xác nhận đặt vé thành công</h1>
                                            <p style="margin: 0; font-size: 14px; color: #6b7280;">Cảm ơn bạn đã đặt vé. Dưới đây là thông tin vé điện tử của bạn.</p>
                                        </td>
                                    </tr>

                                    <!-- Thông tin khách hàng & phim -->
                                    <tr>
                                        <td style="padding: 24px 28px 12px;">
                                            <p style="margin: 0 0 16px; font-size: 14px; line-height: 1.5; color: #374151;">
                                                Xin chào <strong>%s</strong>, giao dịch thanh toán cho đơn hàng của bạn đã hoàn tất.
                                            </p>

                                            <!-- Card tóm tắt phim -->
                                            <div style="background-color: #f9fafb; border: 1px solid #e5e7eb; border-radius: 6px; padding: 16px 18px; margin-bottom: 20px;">
                                                <div style="font-size: 16px; font-weight: 700; color: #111827; margin-bottom: 12px;">%s</div>
                                                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="font-size: 13px; color: #4b5563;">
                                                    <tr>
                                                        <td style="padding: 3px 0; width: 95px; color: #6b7280;">Rạp chiếu:</td>
                                                        <td style="padding: 3px 0; color: #111827; font-weight: 500;">%s</td>
                                                    </tr>
                                                    <tr>
                                                        <td style="padding: 3px 0; color: #6b7280;">Phòng chiếu:</td>
                                                        <td style="padding: 3px 0; color: #111827; font-weight: 500;">%s</td>
                                                    </tr>
                                                    <tr>
                                                        <td style="padding: 3px 0; color: #6b7280;">Suất chiếu:</td>
                                                        <td style="padding: 3px 0; color: #111827; font-weight: 600;">%s</td>
                                                    </tr>
                                                </table>
                                            </div>

                                            <!-- Tiêu đề danh sách vé -->
                                            <div style="font-size: 14px; font-weight: 600; color: #111827; margin-bottom: 10px;">Chi tiết vé đã đặt</div>

                                            <!-- Bảng vé -->
                                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border: 1px solid #e5e7eb; border-radius: 6px; border-collapse: collapse; overflow: hidden;">
                                                <thead>
                                                    <tr style="background-color: #f9fafb; border-bottom: 1px solid #e5e7eb;">
                                                        <th style="padding: 9px 8px; font-size: 12px; font-weight: 600; color: #6b7280; text-align: center; width: 40px;">STT</th>
                                                        <th style="padding: 9px 8px; font-size: 12px; font-weight: 600; color: #6b7280; text-align: center;">Ghế</th>
                                                        <th style="padding: 9px 8px; font-size: 12px; font-weight: 600; color: #6b7280; text-align: center;">Mã vé</th>
                                                        <th style="padding: 9px 8px; font-size: 12px; font-weight: 600; color: #6b7280; text-align: right;">Giá vé</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    %s
                                                </tbody>
                                                <tfoot>
                                                    <tr style="background-color: #f9fafb;">
                                                        <td colspan="3" style="padding: 12px 10px; font-size: 14px; font-weight: 600; color: #111827; text-align: right;">Tổng thanh toán:</td>
                                                        <td style="padding: 12px 10px; font-size: 15px; font-weight: 700; color: #111827; text-align: right;">%s</td>
                                                    </tr>
                                                </tfoot>
                                            </table>

                                            <!-- Lưu ý -->
                                            <div style="margin-top: 20px; padding: 12px 14px; background-color: #f9fafb; border-left: 3px solid #9ca3af; border-radius: 0 4px 4px 0;">
                                                <div style="font-size: 13px; font-weight: 600; color: #374151; margin-bottom: 4px;">Lưu ý khi vào rạp:</div>
                                                <div style="font-size: 13px; color: #6b7280; line-height: 1.5;">
                                                    Vui lòng xuất trình <strong>Mã vé</strong> tại quầy vé hoặc cổng soát vé trước giờ chiếu ít nhất 15 phút để làm thủ tục vào phòng chiếu.
                                                </div>
                                            </div>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td style="padding: 20px 28px 24px; border-top: 1px solid #f3f4f6; text-align: center;">
                                            <p style="margin: 0 0 4px; font-size: 12px; color: #9ca3af;">Email được gửi tự động từ hệ thống đặt vé Cinema.</p>
                                            <p style="margin: 0; font-size: 12px; color: #9ca3af;">Vui lòng không phản hồi thư này.</p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .formatted(
                        safeCustomerName,
                        movieTitle != null ? movieTitle : "Vé Xem Phim",
                        cinemaName != null ? cinemaName : "Hệ thống Rạp Cinema",
                        roomName != null ? roomName : "Phòng Chiếu",
                        formattedTime,
                        ticketRows.toString(),
                        formattedTotal);
    }
}
