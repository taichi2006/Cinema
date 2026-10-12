<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.cinema.payment.service.PaymentService" %>
<%@ page import="com.cinema.payment.dao.PaymentDAO" %>
<%@ page import="com.cinema.payment.entity.Payment" %>
<%@ page import="com.cinema.payment.enums.PaymentType" %>
<%@ page import="com.cinema.payment.enums.PaymentStatus" %>
<%@ page import="com.cinema.payment.gateway.VnPayIpnResponse" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.util.Locale" %>
<%!
    private String getVnPayErrorMessage(String code) {
        if (code == null) return "Giao dịch không thành công hoặc sai chữ ký";
        switch (code) {
            case "07": return "Trừ tiền thành công nhưng giao dịch bị nghi ngờ gian lận";
            case "09": return "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking";
            case "10": return "Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11": return "Đã hết hạn chờ thanh toán";
            case "12": return "Thẻ/Tài khoản của khách hàng bị khóa";
            case "13": return "Quý khách nhập sai mật khẩu xác thực OTP";
            case "24": return "Khách hàng đã hủy giao dịch";
            case "51": return "Tài khoản của quý khách không đủ số dư";
            case "65": return "Tài khoản đã vượt quá hạn mức giao dịch trong ngày";
            case "75": return "Ngân hàng thanh toán đang bảo trì";
            case "79": return "Nhập sai mật khẩu thanh toán quá số lần quy định";
            default: return "Giao dịch không thành công (Mã lỗi: " + code + ")";
        }
    }
%>
<%
    PaymentService paymentService = new PaymentService();
    VnPayIpnResponse ipnResult = null;
    String errorMsg = null;
    
    try {
        ipnResult = paymentService.handleVnPayIpn(request.getParameterMap());
    } catch (Exception e) {
        errorMsg = e.getMessage();
    }

    String vnp_TxnRef = request.getParameter("vnp_TxnRef");
    String vnp_Amount = request.getParameter("vnp_Amount");
    String vnp_OrderInfo = request.getParameter("vnp_OrderInfo");
    String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
    String vnp_TransactionStatus = request.getParameter("vnp_TransactionStatus");
    String vnp_TransactionNo = request.getParameter("vnp_TransactionNo");
    String vnp_BankCode = request.getParameter("vnp_BankCode");
    String vnp_PayDate = request.getParameter("vnp_PayDate");

    long amountVal = 0;
    if (vnp_Amount != null && !vnp_Amount.isBlank()) {
        try {
            amountVal = Long.parseLong(vnp_Amount) / 100;
        } catch (Exception ignored) {}
    }

    Payment paymentObj = null;
    if (vnp_TxnRef != null && !vnp_TxnRef.isBlank()) {
        try {
            long pId = Long.parseLong(vnp_TxnRef);
            paymentObj = new PaymentDAO().findById(pId).orElse(null);
        } catch (Exception ignored) {}
    }

    // Phân biệt giao dịch Nạp tiền ví (TOPUP) hay Thanh toán vé xem phim (BOOKING)
    boolean isTopUp = (paymentObj != null && paymentObj.getPaymentType() == PaymentType.TOPUP)
            || (vnp_OrderInfo != null && vnp_OrderInfo.toLowerCase().contains("nap tien"));

    // Phân định chính xác trạng thái giao dịch
    boolean isVnPaySuccess = "00".equals(vnp_ResponseCode) && (vnp_TransactionStatus == null || "00".equals(vnp_TransactionStatus));
    boolean isCancelled = "24".equals(vnp_ResponseCode);
    boolean isSuccess = isVnPaySuccess && (paymentObj == null || paymentObj.getStatus() == PaymentStatus.SUCCESSFUL);

    String frontendTargetUrl;
    String actionBtnText;
    String actionBtnIcon;

    if (isTopUp) {
        String envWalletUrl = System.getenv("FRONTEND_WALLET_URL");
        frontendTargetUrl = (envWalletUrl != null && !envWalletUrl.isBlank())
                ? envWalletUrl
                : "https://web-final-project-tawny.vercel.app/#/wallet";
        actionBtnText = "Xem lịch sử nạp tiền";
        actionBtnIcon = "fa-wallet";
    } else {
        String envBookingUrl = System.getenv("FRONTEND_BOOKING_URL");
        frontendTargetUrl = (envBookingUrl != null && !envBookingUrl.isBlank())
                ? envBookingUrl
                : "https://web-final-project-tawny.vercel.app/#/user/booking";
        actionBtnText = "Xem vé";
        actionBtnIcon = "fa-ticket";
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= isSuccess ? "Thanh toán thành công" : (isCancelled ? "Đã hủy thanh toán" : "Thanh toán không thành công") %> - Cinema</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
    <style>
        body {
            background: #f0f2f5;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }
        .result-card {
            background: #fff;
            border-radius: 16px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
            max-width: 580px;
            width: 100%;
            overflow: hidden;
            border: none;
        }
        .result-header {
            padding: 32px 24px 20px;
            text-align: center;
        }
        .status-icon {
            width: 72px;
            height: 72px;
            border-radius: 50%;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-size: 36px;
            margin-bottom: 16px;
        }
        .status-success {
            background: #e8f5e9;
            color: #2e7d32;
        }
        .status-cancelled {
            background: #fff3e0;
            color: #e65100;
        }
        .status-failed {
            background: #ffebee;
            color: #c62828;
        }
        .amount-highlight {
            font-size: 28px;
            font-weight: 700;
            margin: 8px 0;
        }
        .amount-success {
            color: #1a237e;
        }
        .amount-cancelled {
            color: #e65100;
        }
        .amount-failed {
            color: #c62828;
        }
        .detail-table {
            margin: 0;
        }
        .detail-table th {
            color: #6c757d;
            font-weight: 500;
            width: 45%;
            padding: 12px 24px;
            border-bottom: 1px solid #f0f0f0;
        }
        .detail-table td {
            color: #212529;
            font-weight: 600;
            padding: 12px 24px;
            border-bottom: 1px solid #f0f0f0;
            text-align: right;
        }
        .btn-action {
            border-radius: 8px;
            padding: 12px 24px;
            font-weight: 600;
        }
    </style>
</head>
<body>

<div class="result-card">
    <div class="result-header">
        <% if (isSuccess) { %>
            <div class="status-icon status-success">
                <i class="fa-solid fa-check"></i>
            </div>
            <h3 class="fw-bold text-success mb-1"><%= isTopUp ? "Nạp tiền thành công!" : "Thanh toán thành công!" %></h3>
            <p class="text-muted mb-2"><%= isTopUp ? "Tiền đã được nạp vào ví của bạn thành công" : "Giao dịch đã được ghi nhận và cập nhật vào hệ thống" %></p>
            <div class="amount-highlight amount-success">
                <%= String.format(Locale.GERMANY, "%,d VNĐ", amountVal) %>
            </div>
        <% } else if (isCancelled) { %>
            <div class="status-icon status-cancelled">
                <i class="fa-solid fa-ban"></i>
            </div>
            <h3 class="fw-bold mb-1" style="color: #e65100;"><%= isTopUp ? "Đã hủy nạp tiền" : "Đã hủy thanh toán" %></h3>
            <p class="text-muted mb-2"><%= isTopUp ? "Bạn đã hủy giao dịch nạp tiền vào ví trên VNPAY" : "Bạn đã hủy giao dịch thanh toán trên VNPAY" %></p>
            <% if (amountVal > 0) { %>
                <div class="amount-highlight amount-cancelled">
                    <%= String.format(Locale.GERMANY, "%,d VNĐ", amountVal) %>
                </div>
            <% } %>
        <% } else { %>
            <div class="status-icon status-failed">
                <i class="fa-solid fa-xmark"></i>
            </div>
            <h3 class="fw-bold text-danger mb-1"><%= isTopUp ? "Nạp tiền không thành công" : "Thanh toán không thành công" %></h3>
            <p class="text-muted mb-2">
                <%= errorMsg != null ? errorMsg : getVnPayErrorMessage(vnp_ResponseCode) %>
            </p>
            <% if (amountVal > 0) { %>
                <div class="amount-highlight amount-failed">
                    <%= String.format(Locale.GERMANY, "%,d VNĐ", amountVal) %>
                </div>
            <% } %>
        <% } %>
    </div>

    <table class="table detail-table">
        <tbody>
            <tr>
                <th><i class="fa-solid fa-receipt me-2"></i>Mã giao dịch (TxnRef)</th>
                <td><%= vnp_TxnRef != null ? vnp_TxnRef : "—" %></td>
            </tr>
            <tr>
                <th><i class="fa-solid fa-comment-dots me-2"></i>Nội dung thanh toán</th>
                <td><%= vnp_OrderInfo != null ? vnp_OrderInfo : "—" %></td>
            </tr>
            <tr>
                <th><i class="fa-solid fa-building-columns me-2"></i>Ngân hàng</th>
                <td><span class="badge bg-primary-subtle text-primary fw-bold"><%= vnp_BankCode != null ? vnp_BankCode : "VNPAY" %></span></td>
            </tr>
            <tr>
                <th><i class="fa-solid fa-hashtag me-2"></i>Mã GD VNPAY</th>
                <td><%= vnp_TransactionNo != null ? vnp_TransactionNo : "—" %></td>
            </tr>
            <tr>
                <th><i class="fa-solid fa-clock me-2"></i>Thời gian</th>
                <td><%= vnp_PayDate != null ? vnp_PayDate : "—" %></td>
            </tr>
            <tr>
                <th><i class="fa-solid fa-shield-halved me-2"></i>Trạng thái xử lý hệ thống</th>
                <td>
                    <% if (isSuccess) { %>
                        <span class="badge bg-success"><i class="fa-solid fa-check me-1"></i>Đã hoàn tất (SUCCESSFUL)</span>
                    <% } else if (isCancelled) { %>
                        <span class="badge bg-warning text-dark"><i class="fa-solid fa-ban me-1"></i>Đã hủy (FAILED)</span>
                    <% } else { %>
                        <span class="badge bg-danger"><i class="fa-solid fa-xmark me-1"></i>Thất bại (FAILED)</span>
                    <% } %>
                </td>
            </tr>
        </tbody>
    </table>

    <div class="p-4 text-center">
        <a href="javascript:window.close();" class="btn btn-outline-secondary btn-action me-2">
            <i class="fa-solid fa-xmark me-1"></i>Đóng
        </a>
        <a href="<%= frontendTargetUrl %>" class="btn btn-primary btn-action" id="btn-view-action">
            <i class="fa-solid <%= actionBtnIcon %> me-1"></i><%= actionBtnText %>
        </a>
    </div>
</div>

<% if (isSuccess) { %>
<script>
    // Tự động chuyển hướng về trang lịch sử tương ứng trên Frontend sau 5 giây nếu thành công
    let countdown = 5;
    const btn = document.getElementById('btn-view-action');
    const targetUrl = '<%= frontendTargetUrl %>';
    const btnText = '<%= actionBtnText %>';
    const btnIcon = '<%= actionBtnIcon %>';
    if (btn) {
        const interval = setInterval(function() {
            countdown--;
            if (countdown > 0) {
                btn.innerHTML = '<i class="fa-solid ' + btnIcon + ' me-1"></i>' + btnText + ' (' + countdown + 's)';
            } else {
                clearInterval(interval);
                window.location.href = targetUrl;
            }
        }, 1000);
    }
</script>
<% } %>

</body>
</html>
