# Hướng dẫn Kiểm thử API bằng Postman

**Dự án**: VNPT Leak Processor  
**Base URL**: `http://localhost:8080`  
**Yêu cầu**: Ứng dụng Spring Boot đang chạy (`mvn spring-boot:run`)

---

## Mục lục

1. [API Giả lập CTIP - Lấy danh sách tài khoản lộ lọt (GET)](#1-api-giả-lập-ctip---lấy-danh-sách-tài-khoản-lộ-lọt-get)
2. [API Giả lập CTIP - Cập nhật trạng thái đóng sự cố (PUT)](#2-api-giả-lập-ctip---cập-nhật-trạng-thái-đóng-sự-cố-put)
3. [API Kích hoạt quét thủ công (POST)](#3-api-kích-hoạt-quét-thủ-công-post)
4. [API Kiểm tra trạng thái Job (GET)](#4-api-kiểm-tra-trạng-thái-job-get)
5. [API Thống kê Dashboard (GET)](#5-api-thống-kê-dashboard-get)
6. [API Test gửi email cảnh báo (GET)](#6-api-test-gửi-email-cảnh-báo-get)
7. [API Test kích hoạt quét tự động (POST)](#7-api-test-kích-hoạt-quét-tự-động-post)
8. [API Test gửi email hàng loạt (POST)](#8-api-test-gửi-email-hàng-loạt-post)
9. [API Test đồng bộ trạng thái CTIP (POST)](#9-api-test-đồng-bộ-trạng-thái-ctip-post)

---

## 1. API Giả lập CTIP - Lấy danh sách tài khoản lộ lọt (GET)

> **Mô tả**: Đây là API giả lập (Mock) của CTIP VNPT thật. Nó trả về danh sách tài khoản bị lộ lọt từ hệ thống.

### Cấu hình Postman

| Thuộc tính | Giá trị |
|---|---|
| **Method** | `GET` |
| **URL** | `http://localhost:8080/api/v1/account-leak/credentials/enrichment` |

### Headers

| Key | Value |
|---|---|
| `accept` | `application/json` |
| `x-key` | `a631d2e4-3b67-4694-96b2-df960826c3b2` |

### Query Params (Tùy chọn)

| Key | Value | Mô tả |
|---|---|---|
| `credential__search` | `ausca.vnpt.vn` | Lọc theo domain trong URL bị lộ |
| `created_at__gte` | `2025-11-01` | Ngày bắt đầu (từ ngày) |
| `created_at__lte` | `2025-12-31` | Ngày kết thúc (đến ngày) |
| `page` | `1` | Số trang (mặc định: 1) |
| `size` | `10` | Số bản ghi mỗi trang (mặc định: 10) |

### Ví dụ Request

**Request 1 — Lấy tất cả (không lọc):**
```
GET http://localhost:8080/api/v1/account-leak/credentials/enrichment?page=1&size=10
```

**Request 2 — Lọc theo domain `ausca.vnpt.vn`:**
```
GET http://localhost:8080/api/v1/account-leak/credentials/enrichment?credential__search=ausca.vnpt.vn&page=1&size=10
```

**Request 3 — Lọc theo khoảng thời gian:**
```
GET http://localhost:8080/api/v1/account-leak/credentials/enrichment?created_at__gte=2025-11-01&created_at__lte=2025-12-31&page=1&size=10
```

### Kết quả mong đợi (200 OK)

```json
{
    "results": [
        {
            "credential_id": "019adf91-e7e5-7061-bf20-3971f1a44ab7",
            "created_at": "2025-12-01T14:09:21.153+07:00",
            "first_compromise_time": "2025-11-03T00:00:00+07:00",
            "url": "https://ausca.vnpt.vn/account/login",
            "username": "066095002466",
            "password": "Tamtri@123",
            "severity": "medium",
            "status_id": "019adf91-e7e5-7061-bf20-3971f1a44ab7",
            "status": "open"
        }
    ],
    "total": 20,
    "page": 1,
    "size": 10,
    "pages": 2
}
```

### Test sai API Key (401 Unauthorized)

Đổi giá trị `x-key` thành chuỗi bất kỳ:

| Key | Value |
|---|---|
| `x-key` | `sai-api-key-12345` |

**Kết quả mong đợi (401):**
```json
{
    "error": "Unauthorized - Invalid or missing x-key header"
}
```

---

## 2. API Giả lập CTIP - Cập nhật trạng thái đóng sự cố (PUT)

> **Mô tả**: Gửi danh sách `status_id` của các sự cố đã xử lý xong lên CTIP để đóng trạng thái. Trong dự án, API này được gọi ở **Bước 3** (sau khi đã gửi email xong).

### Cấu hình Postman

| Thuộc tính | Giá trị |
|---|---|
| **Method** | `PUT` |
| **URL** | `http://localhost:8080/api/v1/account-leak/statuses` |

### Headers

| Key | Value |
|---|---|
| `accept` | `application/json` |
| `Content-Type` | `application/json` |
| `x-key` | `a631d2e4-3b67-4694-96b2-df960826c3b2` |

### Body (raw JSON)

```json
{
    "ids": [
        "019adf91-e7e5-7061-bf20-3971f1a44ab7",
        "019ad8be-9981-7982-9a1c-9010dd8895f5"
    ],
    "status": "close",
    "comment": ""
}
```

> **Lưu ý**: Trường `ids` truyền vào là danh sách các `status_id` (không phải `credential_id`). Có thể gửi 1 hoặc nhiều `status_id` cùng lúc trong mảng `ids`.

### Kết quả mong đợi (200 OK)

```json
{
    "status": "success",
    "message": "Status updated successfully on CTIP platform (Mock success)"
}
```

### Kiểm tra Console Log

Sau khi gửi request, kiểm tra log trong terminal Spring Boot sẽ thấy:
```
Mock CTIP Server: Received PUT /api/v1/account-leak/statuses request. Headers x-key: a631d2e4-...
Mock CTIP Server: Successfully processed PUT request to close incidents.
Updated status: ids = [019adf91-..., 019ad8be-...], status = 'close', comment = ''
```

---

## 3. API Kích hoạt quét thủ công (POST)

> **Mô tả**: Kích hoạt một luồng quét E2E bất đồng bộ: Quét dữ liệu CTIP → Map khách hàng → Gửi email cảnh báo → Đồng bộ CTIP. API trả về ngay `jobId` để theo dõi tiến trình.

### Cấu hình Postman

| Thuộc tính | Giá trị |
|---|---|
| **Method** | `POST` |
| **URL** | `http://localhost:8080/api/v1/jobs/trigger-scan` |

### Headers

Không cần header đặc biệt.

### Kết quả mong đợi (200 OK)

```json
{
    "jobId": 5,
    "status": "RUNNING"
}
```

> **Lưu ý**: Giá trị `jobId` sẽ tăng dần mỗi lần gọi. Sử dụng `jobId` này để theo dõi tiến trình ở API số 4 bên dưới.

---

## 4. API Kiểm tra trạng thái Job (GET)

> **Mô tả**: Kiểm tra trạng thái hiện tại của một Job quét đang chạy hoặc đã hoàn thành. Giao diện Dashboard dùng API này để AJAX Polling cập nhật tiến trình.

### Cấu hình Postman

| Thuộc tính | Giá trị |
|---|---|
| **Method** | `GET` |
| **URL** | `http://localhost:8080/api/v1/jobs/status/{jobId}` |

### Path Variable

| Key | Value | Mô tả |
|---|---|---|
| `jobId` | `5` | ID của Job cần kiểm tra (lấy từ API số 3) |

### Ví dụ Request

```
GET http://localhost:8080/api/v1/jobs/status/5
```

### Kết quả khi Job đang chạy

```json
{
    "jobId": 5,
    "status": "RUNNING",
    "errorMessage": "",
    "totalFetched": 0,
    "totalMapped": 0
}
```

### Kết quả khi Job hoàn thành

```json
{
    "jobId": 5,
    "status": "SUCCESS",
    "errorMessage": "",
    "totalFetched": 11,
    "totalMapped": 5
}
```

### Kết quả khi Job bị lỗi

```json
{
    "jobId": 5,
    "status": "FAILED",
    "errorMessage": "Error fetching leak data from CTIP Server",
    "totalFetched": 0,
    "totalMapped": 0
}
```

### Kết quả khi Job không tồn tại

**Response: 404 Not Found** (body rỗng)

---

## 5. API Thống kê Dashboard (GET)

> **Mô tả**: Trả về toàn bộ số liệu thống kê thời gian thực cho trang Dashboard: tổng số leak, phân loại theo trạng thái và mức độ nghiêm trọng, danh sách 5 bản ghi mới nhất.

### Cấu hình Postman

| Thuộc tính | Giá trị |
|---|---|
| **Method** | `GET` |
| **URL** | `http://localhost:8080/api/v1/jobs/stats` |

### Kết quả mong đợi (200 OK)

```json
{
    "totalLeaks": 11,
    "totalPending": 0,
    "totalEmailed": 0,
    "totalProcessed": 5,
    "totalFailed": 0,
    "totalCustomerNotFound": 6,
    "severityLow": 3,
    "severityMedium": 4,
    "severityHigh": 2,
    "severityCritical": 2,
    "latestLeaks": [
        {
            "credentialId": "019adf91-e7e5-7061-bf20-3971f1a44ab7",
            "username": "066095002466",
            "severity": "medium",
            "customerName": "Nguyễn Văn An",
            "localStatus": "PROCESSED",
            "compromiseTime": "03/11/2025 00:00"
        }
    ]
}
```

### Ý nghĩa các trường

| Trường | Mô tả |
|---|---|
| `totalLeaks` | Tổng số tài khoản lộ lọt đã quét được |
| `totalPending` | Số lượng đang chờ xử lý |
| `totalEmailed` | Số lượng đã gửi email cảnh báo |
| `totalProcessed` | Số lượng đã xử lý hoàn tất (email + đóng CTIP) |
| `totalFailed` | Số lượng gửi email thất bại hoặc CTIP lỗi |
| `totalCustomerNotFound` | Số lượng không tìm thấy khách hàng trong CSDL |
| `severityLow/Medium/High/Critical` | Phân bố theo mức độ nghiêm trọng |

---

## 6. API Test gửi email cảnh báo (GET)

> **Mô tả**: Gửi thử 1 email cảnh báo bảo mật đến địa chỉ email bất kỳ. Dùng để kiểm tra cấu hình SMTP Gmail hoạt động đúng.

### Cấu hình Postman

| Thuộc tính | Giá trị |
|---|---|
| **Method** | `GET` |
| **URL** | `http://localhost:8080/api/v1/test/ctip/email/send` |

### Query Params

| Key | Value | Mô tả |
|---|---|---|
| `to` | `luuminhhien68@gmail.com` | Địa chỉ email người nhận |

### Ví dụ Request

```
GET http://localhost:8080/api/v1/test/ctip/email/send?to=luuminhhien68@gmail.com
```

### Kết quả mong đợi (200 OK)

```json
{
    "status": "success",
    "message": "Static security warning email has been sent successfully to: luuminhhien68@gmail.com"
}
```

> **Kiểm tra**: Vào hộp thư Gmail của người nhận, kiểm tra email cảnh báo bảo mật VNPT SmartCA đã đến với định dạng HTML chính xác.

---

## 7. API Test kích hoạt quét tự động (POST)

> **Mô tả**: Kích hoạt luồng quét giống như scheduler tự động chạy (quét CTIP → lưu DB). Khác với API số 3, API này chạy **đồng bộ** (chờ hoàn thành mới trả kết quả).

### Cấu hình Postman

| Thuộc tính | Giá trị |
|---|---|
| **Method** | `POST` |
| **URL** | `http://localhost:8080/api/v1/test/ctip/scan` |

### Kết quả mong đợi (200 OK)

```json
{
    "status": "success",
    "message": "Triggered credential leak scan job successfully"
}
```

---

## 8. API Test gửi email hàng loạt (POST)

> **Mô tả**: Kích hoạt gửi email cảnh báo cho tất cả các bản ghi rò rỉ đang có trạng thái `PENDING` trong CSDL.

### Cấu hình Postman

| Thuộc tính | Giá trị |
|---|---|
| **Method** | `POST` |
| **URL** | `http://localhost:8080/api/v1/test/ctip/send-emails` |

### Kết quả mong đợi (200 OK)

```json
{
    "status": "success",
    "message": "Triggered sending warning emails to pending leaks successfully"
}
```

> **Lưu ý**: Nếu không còn bản ghi `PENDING` nào trong DB, API vẫn trả thành công nhưng không gửi email nào.

---

## 9. API Test đồng bộ trạng thái CTIP (POST)

> **Mô tả**: Kích hoạt đồng bộ trạng thái `close` lên CTIP cho tất cả bản ghi có trạng thái `EMAIL_SENT` trong CSDL.

### Cấu hình Postman

| Thuộc tính | Giá trị |
|---|---|
| **Method** | `POST` |
| **URL** | `http://localhost:8080/api/v1/test/ctip/sync` |

### Kết quả mong đợi (200 OK)

```json
{
    "status": "success",
    "message": "Triggered closed incident status synchronization to CTIP successfully"
}
```

---

## Luồng kiểm thử E2E hoàn chỉnh bằng Postman

Để kiểm thử toàn bộ luồng nghiệp vụ từ đầu đến cuối, thực hiện theo thứ tự:

### Bước 1: Kiểm tra API CTIP có hoạt động
```
GET /api/v1/account-leak/credentials/enrichment?page=1&size=10
Headers: x-key = a631d2e4-3b67-4694-96b2-df960826c3b2
```
→ Kết quả: Nhận được danh sách tài khoản lộ lọt.

### Bước 2: Kích hoạt quét thủ công
```
POST /api/v1/jobs/trigger-scan
```
→ Kết quả: Nhận được `jobId` và `status: RUNNING`.

### Bước 3: Theo dõi tiến trình Job
```
GET /api/v1/jobs/status/{jobId}
```
→ Gọi lặp lại vài lần cho đến khi `status` chuyển sang `SUCCESS`.

### Bước 4: Kiểm tra thống kê Dashboard
```
GET /api/v1/jobs/stats
```
→ Kết quả: `totalLeaks > 0`, `totalProcessed > 0` (nếu có khách hàng đã map).

### Bước 5: Kiểm tra API PUT đóng CTIP
```
PUT /api/v1/account-leak/statuses
Headers: x-key = a631d2e4-3b67-4694-96b2-df960826c3b2
Body: {"ids": ["<status_id từ bước 1>"], "status": "close", "comment": ""}
```
→ Kết quả: `status: success`.

---

## Bảng tổng hợp tất cả các API

| # | Method | URL | Mô tả | Xác thực |
|---|---|---|---|---|
| 1 | `GET` | `/api/v1/account-leak/credentials/enrichment` | Lấy DS tài khoản lộ lọt từ CTIP | `x-key` header |
| 2 | `PUT` | `/api/v1/account-leak/statuses` | Đóng trạng thái sự cố trên CTIP | `x-key` header |
| 3 | `POST` | `/api/v1/jobs/trigger-scan` | Kích hoạt quét E2E bất đồng bộ | Không |
| 4 | `GET` | `/api/v1/jobs/status/{jobId}` | Kiểm tra trạng thái Job | Không |
| 5 | `GET` | `/api/v1/jobs/stats` | Thống kê Dashboard realtime | Không |
| 6 | `GET` | `/api/v1/test/ctip/email/send?to=...` | Gửi thử 1 email cảnh báo | Không |
| 7 | `POST` | `/api/v1/test/ctip/scan` | Kích hoạt quét (đồng bộ) | Không |
| 8 | `POST` | `/api/v1/test/ctip/send-emails` | Gửi email hàng loạt cho PENDING | Không |
| 9 | `POST` | `/api/v1/test/ctip/sync` | Đồng bộ đóng CTIP cho EMAIL_SENT | Không |
