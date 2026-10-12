<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.cinema.payment.PaymentService" %>
<%@ page import="com.cinema.payment.gateway.VnPayIpnResponse" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.util.Locale" %>
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
    String vnp_TransactionNo = request.getParameter("vnp_TransactionNo");
    String vnp_BankCode = request.getParameter("vnp_BankCode");
    String vnp_PayDate = request.getParameter("vnp_PayDate");

    long amountVal = 0;
    if (vnp_Amount != null && !vnp_Amount.isBlank()) {
        try {
            amountVal = Long.parseLong(vnp_Amount) / 100;
        } catch (Exception ignored) {}
    }

    // 00: Cập nhật thành công, 02: Giao dịch đã được xác nhận thành công trước đó
    boolean isSuccess = ipnResult != null && ("00".equals(ipnResult.rspCode()) || "02".equals(ipnResult.rspCode()));
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kết quả thanh toán VNPAY - Cinema</title>
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
        .status-failed {
            background: #ffebee;
            color: #c62828;
        }
        .amount-highlight {
            font-size: 28px;
            font-weight: 700;
            color: #1a237e;
            margin: 8px 0;
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
            <h3 class="fw-bold text-success mb-1">Thanh toán thành công!</h3>
            <p class="text-muted mb-2">Giao dịch đã được ghi nhận và cập nhật vào hệ thống</p>
            <div class="amount-highlight">
                <%= String.format(Locale.GERMANY, "%,d VNĐ", amountVal) %>
            </div>
        <% } else { %>
            <div class="status-icon status-failed">
                <i class="fa-solid fa-xmark"></i>
            </div>
            <h3 class="fw-bold text-danger mb-1">Thanh toán không thành công</h3>
            <p class="text-muted mb-2">
                <%= ipnResult != null ? ipnResult.message() : (errorMsg != null ? errorMsg : "Giao dịch bị từ chối hoặc sai chữ ký") %>
            </p>
            <% if (amountVal > 0) { %>
                <div class="amount-highlight text-danger">
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
                        <span class="badge bg-success">Đã hoàn tất (SUCCESSFUL)</span>
                    <% } else { %>
                        <span class="badge bg-danger">Chưa hoàn tất (Rsp: <%= ipnResult != null ? ipnResult.rspCode() : "ERR" %>)</span>
                    <% } %>
                </td>
            </tr>
        </tbody>
    </table>

    <div class="p-4 text-center">
        <a href="javascript:window.close();" class="btn btn-outline-secondary btn-action me-2">
            <i class="fa-solid fa-xmark me-1"></i>Đóng
        </a>
        <a href="<%= request.getContextPath() %>/" class="btn btn-primary btn-action">
            <i class="fa-solid fa-house me-1"></i>Về trang chủ
        </a>
    </div>
</div>

</body>
</html>
