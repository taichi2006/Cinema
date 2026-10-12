param (
    [string]$BaseUrl = "https://be-cinema.onrender.com"
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " 🚀 KIỂM TRA TOÀN BỘ API ENDPOINTS - CINEMA BACKEND" -ForegroundColor Cyan
Write-Host " Target URL: $BaseUrl" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

$results = @()
$globalToken = ""
$globalRefreshToken = ""

function Invoke-TestEndpoint {
    param (
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [string]$Token = $null,
        [int[]]$ExpectedStatus = @(200),
        [string]$Description
    )

    $url = "$BaseUrl$Path"
    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $statusCode = 0
    $respBody = $null
    $errorMessage = ""

    try {
        $params = @{
            Uri = $url
            Method = $Method
            Headers = $headers
            ErrorAction = 'Stop'
        }
        if ($Body) {
            $params["ContentType"] = "application/json; charset=utf-8"
            $params["Body"] = ($Body | ConvertTo-Json -Depth 5 -Compress)
        }

        $response = Invoke-WebRequest @params
        $statusCode = [int]$response.StatusCode
        $respBody = $response.Content
    } catch {
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
            $stream = $_.Exception.Response.GetResponseStream()
            if ($stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $respBody = $reader.ReadToEnd()
            }
        } else {
            $errorMessage = $_.Exception.Message
        }
    }
    $sw.Stop()

    $isPass = ($ExpectedStatus -contains $statusCode)
    $statusColor = if ($isPass) { "Green" } else { "Red" }
    $statusLabel = if ($isPass) { "PASS" } else { "FAIL" }

    Write-Host ("[{0,-4}] {1,-6} {2,-35} => Status {3,3} ({4,4}ms) - {5}" -f $statusLabel, $Method, $Path, $statusCode, $sw.ElapsedMilliseconds, $Description) -ForegroundColor $statusColor

    $script:results += [PSCustomObject]@{
        Status      = $statusLabel
        Method      = $Method
        Path        = $Path
        HttpCode    = $statusCode
        DurationMs  = $sw.ElapsedMilliseconds
        Description = $Description
    }

    return $respBody
}

# ========================================================
# 1. TEST PUBLIC GET ENDPOINTS
# ========================================================
Write-Host "--- [1] PUBLIC ENDPOINTS ---" -ForegroundColor Magenta

Invoke-TestEndpoint -Method "GET" -Path "/home" -Description "Dữ liệu trang chủ"
Invoke-TestEndpoint -Method "GET" -Path "/movies" -Description "Danh sách phim"
Invoke-TestEndpoint -Method "GET" -Path "/movies/1" -ExpectedStatus @(200, 404) -Description "Chi tiết phim #1"
Invoke-TestEndpoint -Method "GET" -Path "/movies/1/showtimes?date=2026-10-15" -ExpectedStatus @(200, 404) -Description "Lịch chiếu phim #1"
Invoke-TestEndpoint -Method "GET" -Path "/movies/1/reviews" -ExpectedStatus @(200, 404) -Description "Đánh giá phim #1"
Invoke-TestEndpoint -Method "GET" -Path "/cinemas" -Description "Danh sách rạp chiếu"
Invoke-TestEndpoint -Method "GET" -Path "/cinemas/1" -ExpectedStatus @(200, 404) -Description "Chi tiết rạp #1"
Invoke-TestEndpoint -Method "GET" -Path "/cinemas/1/showtimes?date=2026-10-15" -ExpectedStatus @(200, 404) -Description "Lịch chiếu rạp #1"
Invoke-TestEndpoint -Method "GET" -Path "/showtimes/1" -ExpectedStatus @(200, 404) -Description "Chi tiết suất chiếu #1"
Invoke-TestEndpoint -Method "GET" -Path "/showtimes/1/seats" -ExpectedStatus @(200, 404) -Description "Sơ đồ ghế suất chiếu #1"
Invoke-TestEndpoint -Method "GET" -Path "/products" -Description "Danh sách bắp nước F&B"
Invoke-TestEndpoint -Method "GET" -Path "/vn_return.jsp" -Description "Trang kết quả thanh toán VNPAY"

# ========================================================
# 2. TEST AUTH ENDPOINTS
# ========================================================
Write-Host ""
Write-Host "--- [2] AUTHENTICATION ENDPOINTS ---" -ForegroundColor Magenta

$testEmail = "testuser_auto_" + (Get-Random -Minimum 1000 -Maximum 9999) + "@cinema.vn"
$testPass = "Password123@"

# Thử Register user mới
$regBody = @{
    fullName = "Test User Auto"
    email = $testEmail
    password = $testPass
    phone = "0988776655"
    dob = "2000-01-01"
}
Invoke-TestEndpoint -Method "POST" -Path "/auth/register" -Body $regBody -ExpectedStatus @(201, 409) -Description "Đăng ký tài khoản mới"

# Login
$loginBody = @{
    email = $testEmail
    password = $testPass
}
$loginResp = Invoke-TestEndpoint -Method "POST" -Path "/auth/login" -Body $loginBody -ExpectedStatus @(200) -Description "Đăng nhập tài khoản"

if ($loginResp) {
    try {
        $json = $loginResp | ConvertFrom-Json
        if ($json.data.accessToken) {
            $globalToken = $json.data.accessToken
            $globalRefreshToken = $json.data.refreshToken
            Write-Host "       🔑 Lấy Access Token thành công!" -ForegroundColor Cyan
        }
    } catch {}
}

# Refresh Token
if ($globalRefreshToken) {
    $refBody = @{ refreshToken = $globalRefreshToken }
    Invoke-TestEndpoint -Method "POST" -Path "/auth/refresh" -Body $refBody -ExpectedStatus @(200) -Description "Cấp mới Access Token"
}

# ========================================================
# 3. TEST PROTECTED ENDPOINTS (VỚI JWT TOKEN)
# ========================================================
Write-Host ""
Write-Host "--- [3] PROTECTED ENDPOINTS (AUTH REQUIRED) ---" -ForegroundColor Magenta

if ($globalToken) {
    Invoke-TestEndpoint -Method "GET" -Path "/user" -Token $globalToken -Description "Hồ sơ người dùng"
    Invoke-TestEndpoint -Method "GET" -Path "/user/bookings" -Token $globalToken -Description "Lịch sử đặt vé"
    Invoke-TestEndpoint -Method "GET" -Path "/user/vouchers" -Token $globalToken -Description "Ví voucher"
    Invoke-TestEndpoint -Method "GET" -Path "/wallet" -Token $globalToken -Description "Số dư ví điện tử"
    Invoke-TestEndpoint -Method "GET" -Path "/wallet/transactions" -Token $globalToken -Description "Lịch sử giao dịch ví"
    
    # Nạp tiền ví
    $topupBody = @{ amount = 50000; method = "VNPAY" }
    Invoke-TestEndpoint -Method "POST" -Path "/wallet/top-up" -Body $topupBody -Token $globalToken -ExpectedStatus @(201) -Description "Khởi tạo nạp tiền ví VNPAY"
    
    # Chi tiết booking & vé (ID giả định 1)
    Invoke-TestEndpoint -Method "GET" -Path "/bookings/1" -Token $globalToken -ExpectedStatus @(200, 404, 403) -Description "Chi tiết booking #1"
    Invoke-TestEndpoint -Method "GET" -Path "/tickets/1" -Token $globalToken -ExpectedStatus @(200, 404, 403) -Description "Chi tiết vé #1"
    Invoke-TestEndpoint -Method "GET" -Path "/payments/1" -Token $globalToken -ExpectedStatus @(200, 404, 403) -Description "Chi tiết payment #1"
} else {
    Write-Host "       ⚠️ Bỏ qua test protected vì chưa lấy được JWT Token" -ForegroundColor Yellow
}

# ========================================================
# 4. TEST ERROR HANDLING (401, 404)
# ========================================================
Write-Host ""
Write-Host "--- [4] ERROR HANDLING TESTS ---" -ForegroundColor Magenta

Invoke-TestEndpoint -Method "GET" -Path "/wallet" -ExpectedStatus @(401) -Description "Chặn truy cập khi chưa đăng nhập (Expect 401)"
Invoke-TestEndpoint -Method "GET" -Path "/duong-dan-khong-ton-tai-404" -ExpectedStatus @(404) -Description "Xử lý route 404 chuẩn JSON ErrorResponse"

# ========================================================
# TỔNG KẾT BÁO CÁO
# ========================================================
Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " 📊 BÁO CÁO TỔNG HỢP KIỂM THỬ" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$totalTests = $results.Count
$passed = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$failed = ($results | Where-Object { $_.Status -eq "FAIL" }).Count

Write-Host " Tổng số endpoint kiểm tra : $totalTests"
Write-Host " Thành công (PASS)          : $passed" -ForegroundColor Green
if ($failed -gt 0) {
    Write-Host " Thất bại (FAIL)            : $failed" -ForegroundColor Red
} else {
    Write-Host " Thất bại (FAIL)            : 0" -ForegroundColor Green
}
Write-Host " Tỉ lệ thành công           : $([Math]::Round(($passed / $totalTests) * 100, 2))%" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan
