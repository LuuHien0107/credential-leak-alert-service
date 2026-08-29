package com.vnpt.leakprocessor;

import com.vnpt.leakprocessor.model.JobHistory;
import com.vnpt.leakprocessor.service.TelegramNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
public class TelegramNotificationServiceTests {

    @Autowired
    private TelegramNotificationService telegramNotificationService;

    @Test
    @DisplayName("Kiểm thử gửi thông báo Job 1 qua Telegram Bot không văng exception")
    void testSendJob1Report() {
        JobHistory job = new JobHistory();
        job.setId(99L);
        job.setJobName("TEST_JOB1_TELEGRAM");
        job.setStatus("SUCCESS");
        job.setTotalFetched(5);
        job.setTotalMapped(4);
        job.setStartTime(LocalDateTime.now());
        job.setEndTime(LocalDateTime.now());

        assertDoesNotThrow(() -> telegramNotificationService.sendJob1Report(job, 1));
    }

    @Test
    @DisplayName("Kiểm thử gửi thông báo Job 2 qua Telegram Bot không văng exception")
    void testSendJob2Report() {
        JobHistory job = new JobHistory();
        job.setId(100L);
        job.setJobName("TEST_JOB2_TELEGRAM");
        job.setStatus("SUCCESS");
        job.setTotalSentEmail(4);
        job.setTotalUpdatedCtip(4);
        job.setStartTime(LocalDateTime.now());
        job.setEndTime(LocalDateTime.now());

        assertDoesNotThrow(() -> telegramNotificationService.sendJob2Report(job, 0, 0));
    }

    @Test
    @DisplayName("Kiểm thử gửi thông báo Alert Lỗi qua Telegram Bot không văng exception")
    void testSendJobFailureAlert() {
        JobHistory job = new JobHistory();
        job.setId(101L);
        job.setJobName("TEST_JOB_FAILED_TELEGRAM");
        job.setStatus("FAILED");
        job.setEndTime(LocalDateTime.now());

        assertDoesNotThrow(() -> telegramNotificationService.sendJobFailureAlert(job, "Connection timeout to CTIP"));
    }
}
