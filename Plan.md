# Kế hoạch phát triển và kiểm thử dự án (Dev & Test Plan)
Thời gian thực hiện: 29/07/2026 - 07/08/2026

---

## Giai đoạn 1: Thiết lập hạ tầng & Backend Foundation (29/07 - 31/07)

*   **[x] Công việc 1.1 (Dev - 29/07)**: Khởi tạo dự án Spring Boot, cấu hình kết nối MySQL và tạo các lớp JPA Entities (`Customer`, `CredentialLeak`, `EmailTemplate`, `JobHistory`, `ProcessingLog`) cùng 5 JPA Repositories tương ứng.
*   **[x] Công việc 1.2 (Test - 29/07)**: Viết test khởi động Context (`SpringBootTest`) để kích hoạt Hibernate tự sinh và cấu hình hoàn chỉnh các bảng dữ liệu, ràng buộc khóa ngoại vật lý và chỉ mục trên MySQL `leak_db`.
*   **[x] Công việc 2.1 (Dev - 30/07)**: Xây dựng `CtipClient` giả lập (Mock API) để đọc dữ liệu lộ lọt từ file JSON cục bộ (`mock-ctip-response.json`), hỗ trợ phân trang tự động qua các trang.
*   **[x] Công việc 2.2 (Test - 30/07)**: Viết JUnit Test cho `CtipClient` để kiểm tra khả năng đọc đúng định dạng dữ liệu giả lập, phân trang đầy đủ các bản ghi mà không bị lỗi parse JSON.
*   **[x] Công việc 3.1 (Dev - 31/07)**: Viết logic lập lịch `Spring Scheduler` định kỳ kích hoạt `LeakProcessorService` để quét dữ liệu từ client và lưu trữ xuống database.
*   **[x] Công việc 3.2 (Test - 31/07)**: Viết JUnit Test kiểm tra toàn diện 2 trường hợp:
    - Trường hợp 1: Bản ghi mới (chưa có trong DB) phải được lưu thành công với trạng thái `PENDING`.
    - Trường hợp 2: Bản ghi trùng (đã có trong DB) phải bị bỏ qua (không lưu trùng) và không bị đổi trạng thái về `PENDING`.

---

## Giai đoạn 2: Nghiệp vụ Cảnh báo, Cập nhật & Retry Logic (01/08 - 03/08)

*   **[x] Công việc 4.1 (Dev - 01/08 - 02/08)**: Lập trình `CustomerMappingService` và cấu hình cổng gửi email SMTP qua Gmail cá nhân (sử dụng Google App Password), lưu trữ mẫu HTML tĩnh do thầy cung cấp.
*   **[x] Công việc 4.2 (Test - 02/08)**: Thực hiện gửi thử một email thật từ Gmail cá nhân để xác minh cấu hình SMTP hoạt động tốt, nhận được mail với định dạng HTML hiển thị chính xác.
*   **[x] Công việc 5.1 (Dev - 03/08)**: Xây dựng logic giả lập cập nhật trạng thái đóng (`close`) lên CTIP và cấu hình cơ chế tự động thử lại **Spring Retry** khi gặp lỗi SMTP/CTIP.
*   **[x] Công việc 5.2 (Test - 03/08)**: Viết JUnit Test kiểm tra toàn diện 3 kịch bản:
    - Kịch bản 1 (Happy Path): Gửi thư/Cập nhật thành công ngay lần đầu mà không cần thử lại.
    - Kịch bản 2 (Recoverable): Thử lại tự động thành công ở các lần sau (tối đa 3 lần thử lại) sau khi gặp lỗi kết nối ở lần gọi đầu.
    - Kịch bản 3 (Exhausted): Lỗi liên tục cả 4 lần (gồm 1 lần đầu + 3 lần thử lại), kích hoạt cơ chế dừng lại, báo lỗi và ghi nhận toàn bộ Stack Trace chi tiết vào bảng `processing_logs` của CSDL.

---

## Giai đoạn 3: Phát triển Giao diện Dashboard (04/08 - 05/08)

*   **[x] Công việc 6.1 (Dev - 04/08)**: Xây dựng Web Controller và giao diện Thymeleaf (Dashboard thống kê tiến độ các Job chạy, CRUD Khách hàng, CRUD Email Template).
*   **[x] Công việc 6.2 (Test - 04/08)**: Khởi chạy local, truy cập trình duyệt để kiểm tra trực quan giao diện Dashboard hiển thị chính xác số liệu, thực hiện thử các tính năng CRUD.
*   **[x] Công việc 7.1 (Dev - 05/08)**: Phát triển tính năng kích hoạt quét thủ công (Manual Job Trigger) chạy ngầm bất đồng bộ (`@Async`) và tự động gọi AJAX Polling cập nhật UI.
*   **[x] Công việc 7.2 (Test - 05/08)**: Thao tác bấm nút quét trên giao diện, xác minh trang web không bị đơ và thanh tiến trình tự động cập nhật mượt mạc qua AJAX cho đến khi Job hoàn tất.

---

## Giai đoạn 4: Kiểm thử Tích hợp & Hoàn thiện nộp bài (06/08 - 07/08)

*   **[x] Công việc 8.1 (Dev - 06/08)**: Tích hợp và chạy toàn bộ ứng dụng ở chế độ End-to-End từ giao diện cho đến quét, gửi mail, và đồng bộ trạng thái.
*   **[x] Công việc 8.2 (Test - 06/08)**: Viết Integration Test tổng thể toàn luồng hệ thống để đảm bảo các module hoạt động khớp nối hoàn hảo với nhau.
*   **[ ] Công việc 9.1 (Dev - 07/08)**: Hoàn thiện tài liệu hướng dẫn chạy ứng dụng (`README.md`), tổng hợp ảnh giao diện và nhật ký test để chuẩn bị cho báo cáo.
*   **[ ] Công việc 9.2 (Test - 07/08)**: Đóng gói mã nguồn dự án (file ZIP hoặc Git repository) để nộp bài tập.

---

## Lưu ý đặc biệt cho thiết kế dự án (Bài tập thực hành)

1. **Giao diện Mẫu Email**: Sử dụng mẫu HTML VNPT SmartCA chuyên nghiệp do thầy cung cấp và giữ nguyên y hệt nội dung tĩnh của mẫu thư này, không cần xử lý thay thế các trường thông tin động bằng thẻ Thymeleaf (phần xử lý thay thế động này học sinh sẽ tự triển khai sau).
2. **Cấu hình Mail Server (Gmail cá nhân)**:
   - Hệ thống được cấu hình gửi email cảnh báo bảo mật trực tiếp bằng tài khoản Gmail cá nhân của học sinh thay vì máy chủ mail doanh nghiệp.
   - Sử dụng các tham số cấu hình: `host=smtp.gmail.com`, `port=587`, bật TLS (`starttls.enable=true`) và yêu cầu xác thực (`auth=true`).
   - Mật khẩu được sử dụng phải là **App Password (Mật khẩu ứng dụng)** của Google.
3. **Cơ chế Giả lập API CTIP (Mock API)**:
   - Do chưa có thẩm quyền gọi API thật của VNPT CTIP, hệ thống sẽ thực hiện giả lập dữ liệu trả về (Mock API) thông qua một file JSON cục bộ (`mock-ctip-response.json`) đặt trong thư mục dự án.
   - Cấu trúc JSON trả về của Mock API phải khớp chuẩn 100% với định dạng thực tế của CTIP (chứa các trường: `credential_id` UUID, `username`, `password`, `severity`, `status_id` và `status="open"` - tương ứng theo ảnh mẫu).
   - Tương tự, API PUT cập nhật trạng thái lên CTIP cũng sẽ được giả lập phản hồi thành công và in thông tin log danh sách `status_id` đã được đóng ra console để phục vụ kiểm tra.
