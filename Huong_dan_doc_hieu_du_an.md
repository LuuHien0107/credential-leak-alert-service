# HƯỚNG DẪN ĐỌC HIỂU DỰ ÁN (BACKEND)
## DỰ ÁN: VNPT LEAK PROCESSOR

Tài liệu này được biên soạn nhằm giúp bạn nhanh chóng làm quen, nắm bắt cấu trúc thư mục, thứ tự đọc hiểu mã nguồn và luồng đi của dữ liệu (Dataflow) trong dự án.

---

## 1. Bản Đồ Thư Mục & Thứ Tự Đọc Hiểu File

Để hiểu hệ thống một cách khoa học nhất, bạn nên đọc mã nguồn theo **5 bước** tuần tự dưới đây:

### Bước 1: Đọc tầng dữ liệu (Model/Entity)
*Nằm trong package `com.vnpt.leakprocessor.model`*
*   **Ý nghĩa**: Hiểu cấu trúc các bảng trong CSDL MySQL và mối quan hệ giữa chúng.
*   **Thứ tự đọc**:
    1.  [Customer.java](file:///c:/Users/Admin/Desktop/ThucTap/doc/leak-processor/src/main/java/com/vnpt/leakprocessor/model/Customer.java): Thông tin khách hàng (Username, Email, Số điện thoại...).
    2.  [CredentialLeak.java](file:///c:/Users/Admin/Desktop/ThucTap/doc/leak-processor/src/main/java/com/vnpt/leakprocessor/model/CredentialLeak.java): Bản ghi tài khoản bị rò rỉ (Chứa liên kết `@ManyToOne` đến `Customer`, chứa mật khẩu mã hóa, cấp độ nghiêm trọng, trạng thái xử lý cục bộ và trên CTIP).
    3.  [EmailTemplate.java](file:///c:/Users/Admin/Desktop/ThucTap/doc/leak-processor/src/main/java/com/vnpt/leakprocessor/model/EmailTemplate.java): Lưu trữ mẫu email HTML cảnh báo bảo mật.
    4.  [JobHistory.java](file:///c:/Users/Admin/Desktop/ThucTap/doc/leak-processor/src/main/java/com/vnpt/leakprocessor/model/JobHistory.java): Nhật ký trạng thái của các tiến trình quét (Quét tự động hoặc kích hoạt thủ công).
    5.  [ProcessingLog.java](file:///c:/Users/Admin/Desktop/ThucTap/doc/leak-processor/src/main/java/com/vnpt/leakprocessor/model/ProcessingLog.java): Lưu vết chi tiết (audit logs/lỗi stack trace) khi xử lý từng bản ghi rò rỉ.

### Bước 2: Hiểu cách lấy dữ liệu đầu vào (Client & Mock Server)
*   **Ý nghĩa**: Hiểu cách ứng dụng tương tác với máy chủ CTIP giả lập để lấy danh sách lộ lọt.
*   **Thứ tự đọc**:
    1.  [CtipMockController.java](file:///c:/Users/Admin/Desktop/ThucTap/doc/leak-processor/src/main/java/com/vnpt/leakprocessor/controller/CtipMockController.java): Bộ giả lập máy chủ CTIP. Nếu CSDL trống hoặc chạy JUnit test, nó sẽ đọc từ file JSON tĩnh `mock-ctip-response.json`. Nếu có dữ liệu khách hàng, nó sẽ tự sinh ngẫu nhiên 1-2 leak/khách hàng.
    2.  [CtipClient.java](file:///c:/Users/Admin/Desktop/ThucTap/doc/leak-processor/src/main/java/com/vnpt/leakprocessor/client/CtipClient.java) & [MockCtipClientImpl.java](file:///c:/Users/Admin/Desktop/ThucTap/doc/leak-processor/src/main/java/com/vnpt/leakprocessor/client/impl/MockCtipClientImpl.java): Client HTTP gọi API đến Mock Controller trên để lấy dữ liệu về.

### Bước 3: Đọc dịch vụ gửi thư (Email Service)
*   **Ý nghĩa**: Tìm hiểu cách thức xây dựng cấu hình SMTP và cơ chế tự động thử lại khi gửi mail lỗi.
*   **Thứ tự đọc**:
    1.  [EmailService.java](file:///c:/Users/Admin/Desktop/ThucTap/doc/leak-processor/src/main/java/com/vnpt/leakprocessor/service/EmailService.java) và [EmailServiceImpl.java](file:///c:/Users/Admin/Desktop/ThucTap/doc/leak-processor/src/main/java/com/vnpt/leakprocessor/service/impl/EmailServiceImpl.java).
    2.  **Chú ý**: Lớp này sử dụng `@Retryable` của Spring Retry để tự động thử lại tối đa 3 lần nếu gặp lỗi kết nối SMTP, và phương thức `@Recover` để xử lý ghi nhận khi mọi lần thử lại đều thất bại.

### Bước 4: Đọc lõi nghiệp vụ điều phối (Core Orchestrator Service)
*Đây là file quan trọng nhất chứa toàn bộ logic xử lý chính của dự án.*
*   **File**: [LeakProcessorServiceImpl.java](file:///c:/Users/Admin/Desktop/ThucTap/doc/leak-processor/src/main/java/com/vnpt/leakprocessor/service/impl/LeakProcessorServiceImpl.java).
*   **Nội dung chính**:
    *   `executeScan()`: Lấy dữ liệu từ Mock API, đối chiếu username với bảng `customers` để map thông tin và lưu vào bảng `credential_leaks` với trạng thái `PENDING` (hoặc `CUSTOMER_NOT_FOUND` nếu không khớp).
    *   `sendWarningEmails()`: Quét các bản ghi `PENDING`, gửi email cho khách hàng, cập nhật trạng thái cục bộ thành `EMAIL_SENT`.
    *   `sendEmailForLeak()`: Phương thức con gửi email độc lập được đánh dấu `@Transactional(propagation = Propagation.REQUIRES_NEW)` để đảm bảo mỗi khách hàng được xử lý và ghi nhận ngay lập tức vào DB (phục vụ cập nhật real-time).
    *   `syncClosedStatusToCtip()`: Thu thập các bản ghi `EMAIL_SENT`, gọi API PUT lên CTIP để đồng bộ đóng sự cố, cập nhật trạng thái cục bộ thành `PROCESSED`.
    *   `processLeaks()` (Luồng chạy tuần tự đồng bộ) & `processLeaksAsync()` (Luồng bất đồng bộ `@Async` chạy nền khi người dùng ấn nút quét trên Dashboard).

### Bước 5: Đọc cơ chế kích hoạt (Schedulers & Controllers)
*   **Ý nghĩa**: Hiểu cách hệ thống kích hoạt luồng xử lý (tự động theo thời gian hay thủ công qua API).
*   **Thứ tự đọc**:
    1.  [LeakProcessorScheduler.java](file:///c:/Users/Admin/Desktop/ThucTap/doc/leak-processor/src/main/java/com/vnpt/leakprocessor/scheduler/LeakProcessorScheduler.java): Lập lịch tự động bằng `@Scheduled(cron = "...")` chạy định kỳ gọi các bước quét và gửi email.
    2.  [JobRestController.java](file:///c:/Users/Admin/Desktop/ThucTap/doc/leak-processor/src/main/java/com/vnpt/leakprocessor/controller/JobRestController.java): REST API tiếp nhận yêu cầu kích hoạt quét thủ công từ giao diện (`/api/v1/jobs/trigger-scan`), trả về `jobId` ngay lập tức để phía Client gọi API kiểm tra trạng thái (`/api/v1/jobs/status/{jobId}`) hoặc lấy thống kê số liệu Dashboard (`/api/v1/jobs/stats`).

---

## 2. Các Luồng Nghiệp Vụ Chính (Dataflow Diagrams)

### Luồng Quét & Xử Lý Tự Động Định Kỳ (Spring Scheduler)

```
[LeakProcessorScheduler] (Kích hoạt tự động)
         │
         ▼
[LeakProcessorServiceImpl.processLeaks()]
         │
         ├──► Bước 1: executeScan()
         │             ├── Gọi CtipClient.fetchLeaks()
         │             ├── Map Username sang Customer
         │             └── Lưu DB: PENDING (nếu có khách hàng) hoặc CUSTOMER_NOT_FOUND
         │
         ├──► Bước 2: sendWarningEmails()
         │             ├── Lấy các leak PENDING
         │             ├── Gọi EmailService gửi thư cảnh báo bảo mật
         │             └── Lưu DB: EMAIL_SENT (nếu thành công) hoặc EMAIL_FAILED
         │
         └──► Bước 3: syncClosedStatusToCtip()
                       ├── Lấy các leak EMAIL_SENT
                       ├── Gọi CtipClient.updateCtipStatus() đóng sự cố trên CTIP
                       └── Lưu DB: PROCESSED (nếu thành công) hoặc CTIP_UPDATE_FAILED
```

---

### Luồng Bất Đồng Bộ Khi Quét Thủ Công Từ Dashboard (Manual E2E)

Khi Admin nhấn nút **"Quét thủ công"** trên giao diện:

```
[UI Dashboard] ───► POST /api/v1/jobs/trigger-scan ───► [JobRestController]
                                                               │
                                         (Tạo JobHistory & Khởi chạy @Async ngầm)
                                                               │
                                                               ▼
                                                  [LeakProcessorServiceImpl]
                                                  .processLeaksAsync(job)
                                                               │
     ┌─────────────────────────────────────────────────────────┴────────────────────────────────────────────────────────┐
     ▼ (Bước 1: Quét nhanh trong ~1s)                                                                                    ▼ (Bước 2 & 3 chạy ngầm)
executeScan()                                                                                                       sendWarningEmails()
  └── Set Job status = "SUCCESS"                                                                                     └── Gửi email từng tài khoản
  └── (Frontend Polling nhận kết quả SUCCESS tắt màn hình chờ)                                                        └── syncClosedStatusToCtip()
```

---

## 3. Các Công Nghệ & Giải Pháp Kỹ Thuật Độc Đáo Của Dự Án

### 3.1. Cơ chế Gọi Giả lập Tự phục vụ (Self-loop Mock API)
Do không có cổng kết nối trực tiếp đến máy chủ thật của CTIP, dự án đã triển khai **CtipMockController** hoạt động song song trên cổng `8080`.
*   File cấu hình `application.properties` trỏ link API về chính mình: `ctip.api.url=http://localhost:8080`.
*   Giúp dự án chạy độc lập mà không cần cấu hình thêm mock server bên ngoài (như WireMock).

### 3.2. Cô Lập Cơ Sở Dữ Liệu Khi Kiểm Thử (H2 In-Memory DB)
*   Để các ca kiểm thử JUnit không ghi đè hoặc xóa sạch dữ liệu trên MySQL thật (`leak_db`), dự án cấu hình profile riêng trong file `application-test.properties`.
*   Khi chạy lệnh `mvn test`, ứng dụng tự động khởi chạy CSDL H2 lưu trong RAM, tự giải phóng sau khi kiểm thử kết thúc.

### 3.3. Cơ chế Kháng Lỗi (Spring Retry & Recovery)
*   Sử dụng `@EnableRetry` kết hợp `@Retryable` trong `EmailServiceImpl` và `MockCtipClientImpl`.
*   Nếu đường truyền mạng bị chập chờn, hệ thống tự động tạm ngưng và thực hiện kết nối lại tối đa 3 lần. Nếu lỗi liên tục, cơ chế `@Recover` sẽ bắt lỗi, ghi nhận toàn bộ chi tiết vết lỗi (Stack Trace) vào bảng `processing_logs` nhằm giúp Quản trị viên dễ dàng tra cứu.

### 3.4. Transaction Độc Lập cho Từng Khách hàng (Propagation.REQUIRES_NEW)
*   Để tránh tình trạng "xử lý hết toàn bộ mới lưu một thể" khiến người dùng chờ đợi quá lâu, logic gửi mail được phân tách thành từng Transaction nhỏ cho mỗi tài khoản lộ lọt.
*   Trạng thái của tài khoản lộ lọt sẽ cập nhật lập tức từ `PENDING` sang `EMAIL_SENT` ngay khi email của người đó được gửi thành công, giúp màn hình Dashboard hiển thị tiến độ tăng dần trong thời gian thực.
