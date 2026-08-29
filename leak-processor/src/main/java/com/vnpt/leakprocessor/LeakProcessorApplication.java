package com.vnpt.leakprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Lớp khởi chạy chính của ứng dụng Spring Boot VNPT Leak Processor.
 * Kích hoạt các tính năng lập lịch tự động (@EnableScheduling), chạy bất đồng bộ (@EnableAsync), và tự động thử lại khi lỗi (@EnableRetry).
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableRetry
public class LeakProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeakProcessorApplication.class, args);
    }
}
