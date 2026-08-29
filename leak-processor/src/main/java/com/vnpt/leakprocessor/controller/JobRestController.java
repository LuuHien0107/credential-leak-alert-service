package com.vnpt.leakprocessor.controller;

import com.vnpt.leakprocessor.model.JobHistory;
import com.vnpt.leakprocessor.service.DashboardService;
import com.vnpt.leakprocessor.service.LeakFetchService;
import com.vnpt.leakprocessor.service.LeakNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller cung cấp các API để kiểm soát tiến trình chạy Job và lấy số liệu thống kê cho Dashboard.
 * Đã được chuẩn hóa architecture: Ủy quyền toàn bộ việc tra cứu CSDL cho tầng DashboardService.
 */
@RestController
@RequestMapping("/api/v1/jobs")
public class JobRestController {

    private final DashboardService dashboardService;
    private final LeakFetchService leakFetchService;
    private final LeakNotificationService leakNotificationService;

    public JobRestController(
            DashboardService dashboardService,
            LeakFetchService leakFetchService,
            LeakNotificationService leakNotificationService) {
        this.dashboardService = dashboardService;
        this.leakFetchService = leakFetchService;
        this.leakNotificationService = leakNotificationService;
    }

    /**
     * Kích hoạt bất đồng bộ Job 1: Quét dữ liệu rò rỉ từ CTIP API và Xác thực SmartCA.
     */
    @PostMapping("/trigger-job1")
    public ResponseEntity<Map<String, Object>> triggerJob1() {
        JobHistory job = dashboardService.createAndSaveJobHistory("MANUAL_JOB1_FETCH_VERIFY");

        leakFetchService.executeJob1Async(job);

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", job.getId());
        response.put("status", job.getStatus());
        response.put("jobName", "Job 1: Quét CTIP & Xác thực SmartCA");
        return ResponseEntity.ok(response);
    }

    /**
     * Kích hoạt bất đồng bộ Job 2: Gửi email cảnh báo bảo mật và Đồng bộ đóng sự cố lên CTIP.
     */
    @PostMapping("/trigger-job2")
    public ResponseEntity<Map<String, Object>> triggerJob2() {
        JobHistory job = dashboardService.createAndSaveJobHistory("MANUAL_JOB2_EMAIL_SYNC");

        leakNotificationService.executeJob2Async(job);

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", job.getId());
        response.put("status", job.getStatus());
        response.put("jobName", "Job 2: Gửi Email & Đóng CTIP");
        return ResponseEntity.ok(response);
    }

    /**
     * Legacy endpoint kích hoạt Job 1 (tương thích ngược).
     */
    @PostMapping("/trigger-scan")
    public ResponseEntity<Map<String, Object>> triggerScan() {
        return triggerJob1();
    }

    /**
     * Lấy trạng thái hiện tại của một Job cụ thể dựa theo ID (Polling).
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable("jobId") Long jobId) {
        JobHistory job = dashboardService.getJobById(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", job.getId());
        response.put("status", job.getStatus());
        response.put("jobName", job.getJobName());
        response.put("errorMessage", job.getErrorMessage() != null ? job.getErrorMessage() : "");
        response.put("totalFetched", job.getTotalFetched());
        response.put("totalMapped", job.getTotalMapped());
        response.put("totalSentEmail", job.getTotalSentEmail());
        response.put("totalUpdatedCtip", job.getTotalUpdatedCtip());
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy dữ liệu thống kê tổng hợp thời gian thực phục vụ hiển thị biểu đồ và các ô số liệu trên Dashboard.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = dashboardService.getDashboardStatsMap();
        return ResponseEntity.ok(stats);
    }
}
