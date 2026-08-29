package com.vnpt.leakprocessor.client.impl;

import com.vnpt.leakprocessor.client.CtipClient;
import com.vnpt.leakprocessor.dto.CtipLeakResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Lớp gọi API thực tế tới cổng thông tin VNPT CTIP qua giao thức HTTP (sử dụng RestTemplate).
 */
@Service
public class CtipClientImpl implements CtipClient {

    private static final Logger logger = LoggerFactory.getLogger(CtipClientImpl.class);

    private final RestTemplate restTemplate;
    private final String ctipApiKey;
    private final String enrichmentUrl;
    private final String statusesUrl;

    public CtipClientImpl(
            @Qualifier("ctipRestTemplate") RestTemplate restTemplate,
            @Value("${ctip.api.key}") String ctipApiKey,
            @Value("${ctip.api.enrichment-url}") String enrichmentUrl,
            @Value("${ctip.api.statuses-url}") String statusesUrl) {
        this.restTemplate = restTemplate;
        this.ctipApiKey = ctipApiKey;
        this.enrichmentUrl = enrichmentUrl;
        this.statusesUrl = statusesUrl;
    }

    /**
     * Tải danh sách thông tin tài khoản bị lộ lọt từ hệ thống CTIP.
     * Tích hợp cơ chế tự động thử lại (maxAttempts = 3) nếu kết nối gặp lỗi.
     */
    @Override
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public CtipLeakResponse fetchLeaks(String credentialSearch, LocalDate createdAtGte, LocalDate createdAtLte, int page, int size) {
        logger.info("CtipClient: Dang gui HTTP GET yeu cau tai du lieu ro ri tu may chu CTIP ({}) ...", enrichmentUrl);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(enrichmentUrl)
                .queryParam("order_by", "-created_at")
                .queryParam("page", page)
                .queryParam("size", size);

        if (credentialSearch != null && !credentialSearch.trim().isEmpty()) {
            builder.queryParam("credential__search", credentialSearch);
        }
        if (createdAtGte != null) {
            builder.queryParam("created_at__gte", createdAtGte.toString());
        }
        if (createdAtLte != null) {
            builder.queryParam("created_at__lte", createdAtLte.toString());
        }

        String finalUrl = builder.toUriString();

        // Cấu hình các Header xác thực quyền truy cập
        HttpHeaders headers = new HttpHeaders();
        headers.set("accept", "application/json");
        headers.set("x-key", ctipApiKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<CtipLeakResponse> response = restTemplate.exchange(
                    finalUrl,
                    HttpMethod.GET,
                    entity,
                    CtipLeakResponse.class
            );

            logger.info("CtipClient: Tai du lieu lo lot thanh cong qua HTTP GET. Trang thai phan hoi: {}", response.getStatusCode());
            return response.getBody();
        } catch (Exception e) {
            logger.error("CtipClient: Loi phat sinh khi goi HTTP GET API credentials/enrichment: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi kết nối khi lấy dữ liệu lộ lọt từ máy chủ CTIP", e);
        }
    }

    /**
     * Cập nhật trạng thái đóng ("close") các sự cố tài khoản lộ lọt đã xử lý xong lên hệ thống CTIP.
     * Tích hợp cơ chế tự động thử lại (maxAttempts = 3) nếu kết nối gặp lỗi.
     */
    @Override
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void updateCtipStatus(List<String> ids, String status) {
        logger.info("CtipClient: Dang gui HTTP PUT yeu cau cap nhat trang thai su co ({}) cho danh sach: {}", statusesUrl, ids);

        // Cấu hình các Header và kiểu Content-Type JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("accept", "application/json");
        headers.set("x-key", ctipApiKey);

        // Khởi tạo Payload gửi yêu cầu đóng sự cố (ids, status và comment rỗng)
        Map<String, Object> payload = Map.of(
                "ids", ids,
                "status", status,
                "comment", ""
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    statusesUrl,
                    HttpMethod.PUT,
                    entity,
                    Void.class
            );
            logger.info("CtipClient: Dong bo va dong su co thanh cong len CTIP. Trang thai phan hoi: {}", response.getStatusCode());
        } catch (Exception e) {
            logger.error("CtipClient: Loi phat sinh khi goi HTTP PUT API statuses: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi kết nối khi đồng bộ trạng thái đóng sự cố lên máy chủ CTIP", e);
        }
    }
}
