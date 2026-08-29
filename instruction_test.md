# HƯỚNG DẪN KIỂM THỬ HỆ THỐNG (TESTING GUIDE)
## Hệ thống Cảnh báo và Cập nhật Trạng thái Tài khoản Lộ lọt từ VNPT CTIP
*Cập nhật: 05/08/2026*

Hệ thống hỗ trợ cả 2 chế độ: chạy mô phỏng cục bộ (**Local Mock HTTP**) và chạy thực tế kết nối trực tiếp đến cổng dịch vụ thật của VNPT (**Production HTTP**).

---

## 1. Cách cấu hình chế độ chạy trong `application.properties`

Trước khi bắt đầu khởi chạy, bạn hãy mở file [application.properties](file:///c:/Users/Admin/Desktop/ThucTap/doc/leak-processor/src/main/resources/application.properties) để cấu hình chế độ tương ứng:

### A. Chế độ MÔ PHỎNG CỤC BỘ (Khuyên dùng khi Test)
*   **Mục đích**: Hệ thống sẽ gọi API giả lập chạy trên localhost cổng `8080`.
*   **Cấu hình**:
    ```properties
    # VNPT CTIP API configurations
    ctip.api.url=http://localhost:8080
    ctip.api.key=a631d2e4-3b67-4694-96b2-df960826c3b2
    ```

### B. Chế độ CHẠY THẬT (Production)
*   **Mục đích**: Kết nối trực tiếp và gửi/nhận dữ liệu thực tế tới máy chủ VNPT CTIP.
*   **Cấu hình**:
    ```properties
    # VNPT CTIP API configurations
    ctip.api.url=https://ctip.vnpt.vn/api/v1/account-leak/credentials/enrichment
    ctip.api.key=a631d2e4-3b67-4694-96b2-df960826c3b2
    ```

---

## 2. Các bước khởi chạy và kiểm thử

### Bước 1: Đóng gói và Chạy ứng dụng
Mở Terminal tại thư mục `c:\Users\Admin\Desktop\ThucTap\doc\leak-processor\`, thực hiện các lệnh:
```powershell
# 1. Biên dịch và đóng gói (bỏ qua chạy thử unit test tự động)
$env:MAVEN_OPTS="-Dfile.encoding=UTF-8"; mvn clean package -DskipTests

# 2. Khởi chạy file JAR
java -jar target/leak-processor-0.0.1-SNAPSHOT.jar
```

### Bước 2: Chuẩn bị dữ liệu khách hàng trong CSDL MySQL
Mở MySQL Workbench hoặc DBeaver, kết nối CSDL và chạy câu lệnh insert một khách hàng mẫu trùng với tài khoản lộ lọt trong danh sách kiểm thử:
```sql
-- Thêm thông tin email của bạn để kiểm tra tính năng gửi mail thật
INSERT INTO leak_db.customers (username, email, full_name, created_at, updated_at) 
VALUES ('066095002466', 'luuminhhien68@gmail.com', 'Lưu Minh Hiền', NOW(), NOW());
```

### Bước 3: Kiểm thử luồng End-to-End bằng Postman
Bạn gọi lần lượt 3 API sau theo thứ tự:

1.  **Quét và lưu dữ liệu (Scan & Fetch)**:
    *   **Method**: `POST`
    *   **URL**: `http://localhost:8080/api/v1/test/ctip/scan`
    *   *Mô tả*: Hệ thống thực hiện gọi HTTP GET tới API CTIP để lấy danh sách tài khoản lộ lọt về, kiểm tra lọc trùng.
    *   *Xác minh*: Bản ghi `066095002466` được lưu vào bảng `credential_leaks` với trạng thái `PENDING` (mật khẩu đã được mã hóa AES). Bản ghi không có khách hàng sẽ lưu trạng thái `CUSTOMER_NOT_FOUND`.

2.  **Gửi email cảnh báo (Send Warning Email)**:
    *   **Method**: `POST`
    *   **URL**: `http://localhost:8080/api/v1/test/ctip/send-emails`
    *   *Mô tả*: Hệ thống duyệt các leaks `PENDING`, gửi email HTML cảnh báo qua SMTP Gmail.
    *   *Xác minh*: Bạn sẽ nhận được 1 email cảnh báo thật trong Inbox Gmail của mình. Trạng thái bản ghi trong DB đổi thành `EMAIL_SENT`.

3.  **Đồng bộ đóng sự cố lên CTIP (Sync Close Status)**:
    *   **Method**: `POST`
    *   **URL**: `http://localhost:8080/api/v1/test/ctip/sync`
    *   *Mô tả*: Hệ thống gom các status_id của các bản ghi `EMAIL_SENT` (hoặc `CTIP_UPDATE_FAILED`), gọi HTTP PUT đóng sự cố lên VNPT CTIP.
    *   *Xác minh*: Các bản ghi trong CSDL chuyển sang trạng thái thành công cuối cùng là `PROCESSED`.

---

## 3. Cách kiểm thử các trường hợp đặc biệt

### A. Kiểm thử cơ chế Thử lại (Spring Retry)
1.  Đổi cấu hình SMTP Host sang địa chỉ sai để cố tình gây lỗi kết nối gửi thư:
    `spring.mail.host=smtp.gmail.com.fail`
2.  Khởi chạy lại ứng dụng và gọi API gửi thư: `POST http://localhost:8080/api/v1/test/ctip/send-emails`
3.  **Kết quả**: Trên Terminal của IDE bạn sẽ thấy hệ thống tự động thử lại 3 lần nữa sau lần gọi đầu tiên (mỗi lần cách nhau 2s, 4s, 8s). Trạng thái bản ghi trong CSDL sẽ chuyển thành `EMAIL_FAILED` và ghi vết stack trace chi tiết.

### B. Kiểm thử tính năng Tự phục hồi (Self-Healing)
1.  Trong khi chạy chế độ Local Mock, đổi `ctip.api.key` sang key sai: `ctip.api.key=key-sai-cố-tình` để gây lỗi xác thực khi gọi PUT đồng bộ lên CTIP.
2.  Gửi request đồng bộ: `POST http://localhost:8080/api/v1/test/ctip/sync` $\rightarrow$ Hệ thống báo lỗi và cập nhật trạng thái các leaks thành `CTIP_UPDATE_FAILED`.
3.  Đổi lại `ctip.api.key` đúng $\rightarrow$ Khởi chạy lại $\rightarrow$ Gọi lại API đồng bộ $\rightarrow$ Hệ thống tự động quét bù những leaks bị lỗi `CTIP_UPDATE_FAILED` ở bước trước và cập nhật thành công lên CTIP, chuyển trạng thái sang `PROCESSED`.
