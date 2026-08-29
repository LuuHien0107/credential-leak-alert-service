package com.vnpt.leakprocessor.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vnpt.leakprocessor.dto.CtipLeakResponse;
import com.vnpt.leakprocessor.dto.CtipLeakResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller giả lập (Mock) cho các API của cổng thông tin rò rỉ VNPT CTIP.
 * Giúp ứng dụng chạy thử nghiệm và kiểm thử offline một cách độc lập mà không
 * cần kết nối thật.
 */
@RestController
@RequestMapping("/api/v1/account-leak")
public class CtipMockController {

    private static final Logger logger = LoggerFactory.getLogger(CtipMockController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String API_KEY = "a631d2e4-3b67-4694-96b2-df960826c3b2";

    public CtipMockController() {
    }

    /**
     * Endpoint giả lập API lấy danh sách thông tin tài khoản bị lộ lọt
     * (Enrichment).
     * URL: GET /api/v1/account-leak/credentials/enrichment
     *
     * @param xKey   mã API Key truyền vào từ Header để xác thực quyền truy cập.
     * @param search từ khóa tìm kiếm theo URL/tên miền của tài khoản bị lộ.
     * @param gte    ngày bắt đầu lọc (lớn hơn hoặc bằng).
     * @param lte    ngày kết thúc lọc (nhỏ hơn hoặc bằng).
     * @param page   số thứ tự trang cần lấy dữ liệu (phục vụ phân trang).
     * @param size   số lượng bản ghi tối đa trên một trang.
     * @return danh sách các bản ghi rò rỉ đã phân trang và lọc theo điều kiện dưới
     *         dạng JSON.
     */
    @GetMapping("/credentials/enrichment")
    public ResponseEntity<?> fetchLeaks(
            @RequestHeader(value = "x-key", required = false) String xKey,
            @RequestParam(value = "credential__search", required = false, defaultValue = "") String search,
            @RequestParam(value = "created_at__gte", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate gte,
            @RequestParam(value = "created_at__lte", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lte,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {

        logger.info("Mock CTIP Server: Nhan duoc yeu cau GET lay danh sach lo lot tai khoan.");

        // Xác thực mã API Key trong Header
        if (xKey == null || !xKey.equals(API_KEY)) {
            logger.warn("Mock CTIP Server: Yeu cau bi tu choi do khong hop le hoac thieu Header x-key: {}", xKey);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized - Invalid or missing x-key header"));
        }

        try {
            List<CtipLeakResult> allLeaks = new ArrayList<>();
            ClassPathResource resource = new ClassPathResource("mock-ctip-response.json");
            try (InputStream is = resource.getInputStream()) {
                allLeaks = objectMapper.readValue(is, new TypeReference<List<CtipLeakResult>>() {
                });
            }

            // Lọc kết quả theo tên miền tìm kiếm trong URL rò rỉ
            List<CtipLeakResult> filteredLeaks = allLeaks;
            if (search != null && !search.trim().isEmpty()) {
                filteredLeaks = filteredLeaks.stream()
                        .filter(leak -> leak.getUrl() != null
                                && leak.getUrl().toLowerCase().contains(search.toLowerCase()))
                        .collect(Collectors.toList());
            }

            // Lọc kết quả theo khoảng thời gian rò rỉ được ghi nhận (created_at)
            if (gte != null || lte != null) {
                filteredLeaks = filteredLeaks.stream()
                        .filter(leak -> {
                            if (leak.getCreatedAt() == null)
                                return false;
                            try {
                                LocalDate leakDate = OffsetDateTime.parse(leak.getCreatedAt()).toLocalDate();
                                if (gte != null && leakDate.isBefore(gte))
                                    return false;
                                if (lte != null && leakDate.isAfter(lte))
                                    return false;
                                return true;
                            } catch (Exception ex) {
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
            }

            // Thực hiện tính toán phân trang dữ liệu trả về
            int total = filteredLeaks.size();
            int pages = (int) Math.ceil((double) total / size);
            if (pages == 0) {
                pages = 1;
            }

            int fromIndex = (page - 1) * size;
            List<CtipLeakResult> paginatedList = new ArrayList<>();
            if (fromIndex >= 0 && fromIndex < total) {
                int toIndex = Math.min(fromIndex + size, total);
                paginatedList = filteredLeaks.subList(fromIndex, toIndex);
            }

            CtipLeakResponse response = new CtipLeakResponse(paginatedList, total, page, size, pages);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Mock CTIP Server: Gap loi trong qua trinh xu ly tai du lieu gia lap", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Endpoint giả lập API cập nhật trạng thái đóng sự cố rò rỉ tài khoản trên hệ
     * thống CTIP.
     * URL: PUT /api/v1/account-leak/statuses
     *
     * @param xKey    mã API Key truyền từ Header để xác thực quyền truy cập.
     * @param payload chứa danh sách status_id cần đóng, trạng thái mới và ghi chú.
     * @return trạng thái thành công của yêu cầu đóng sự cố.
     */
    @PutMapping("/statuses")
    public ResponseEntity<?> updateStatuses(
            @RequestHeader(value = "x-key", required = false) String xKey,
            @RequestBody Map<String, Object> payload) {

        logger.info("Mock CTIP Server: Nhan yeu cau PUT cap nhat trang thai su co. Headers x-key: {}", xKey);

        // Xác thực mã API Key trong Header
        if (xKey == null || !xKey.equals(API_KEY)) {
            logger.warn("Mock CTIP Server: Tu choi yeu cau PUT do API Key khong dung: {}", xKey);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized - Invalid or missing x-key header"));
        }

        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) payload.get("ids");
        String status = (String) payload.get("status");
        String comment = (String) payload.get("comment");

        logger.info("Mock CTIP Server: Xu ly thanh cong yeu cau cap nhat trang thai dong su co.");
        logger.info("Chi tiet cap nhat: danh sach ids = {}, trang thai moi = '{}', ghi chu = '{}'", ids, status,
                comment);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Status updated successfully on CTIP platform (Mock success)"));
    }
}
