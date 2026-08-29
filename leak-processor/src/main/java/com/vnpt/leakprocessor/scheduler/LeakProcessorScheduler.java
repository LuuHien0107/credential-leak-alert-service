package com.vnpt.leakprocessor.scheduler;

import com.vnpt.leakprocessor.service.LeakFetchService;
import com.vnpt.leakprocessor.service.LeakNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Lớp điều phối các Job tự động kích hoạt lập lịch định kỳ (Scheduler) độc lập cho Job 1 và Job 2.
 */
@Component
public class LeakProcessorScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LeakProcessorScheduler.class);

    private final LeakFetchService leakFetchService;
    private final LeakNotificationService leakNotificationService;

    public LeakProcessorScheduler(
            LeakFetchService leakFetchService,
            LeakNotificationService leakNotificationService) {
        this.leakFetchService = leakFetchService;
        this.leakNotificationService = leakNotificationService;
    }

    /**
     * Job 1 tự động: Quét dữ liệu rò rỉ từ CTIP API, xác thực đăng nhập SmartCA và lưu dữ liệu PENDING.
     * Cấu hình biểu thức Cron trong application.properties: job1.scan.cron (Mặc định: 12h đêm hàng ngày "0 0 0 * * *").
     */
    @Scheduled(cron = "${job1.scan.cron:0 0 0 * * *}")
    public void runJob1ScanAndVerify() {
        logger.info("Scheduler: Kich hoat Job 1 dinh ky (Scan CTIP & Verify SmartCA)...");
        try {
            leakFetchService.executeJob1FetchAndVerify();
            logger.info("Scheduler: Job 1 dinh ky hoan tat thanh cong.");
        } catch (Exception e) {
            logger.error("Scheduler: Job 1 dinh ky gap loi khi thuc thi: {}", e.getMessage(), e);
        }
    }

    /**
     * Job 2 tự động: Gửi email cảnh báo bảo mật cho các sự cố PENDING và đồng bộ đóng sự cố lên CTIP API.
     * Cấu hình biểu thức Cron trong application.properties: job2.email.cron (Mặc định: 12h đêm mỗi 3 ngày).
     */
    @Scheduled(cron = "${job2.email.cron:0 0 0 */3 * *}")
    public void runJob2SendEmailAndSync() {
        logger.info("Scheduler: Kich hoat Job 2 dinh ky (Send Email & CTIP Sync)...");
        try {
            leakNotificationService.executeJob2SendEmailsAndSyncCtip();
            logger.info("Scheduler: Job 2 dinh ky hoan tat thanh cong.");
        } catch (Exception e) {
            logger.error("Scheduler: Job 2 dinh ky gap loi khi thuc thi: {}", e.getMessage(), e);
        }
    }
}
