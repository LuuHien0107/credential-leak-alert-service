package com.vnpt.leakprocessor.service;

import com.vnpt.leakprocessor.model.JobHistory;
import java.util.concurrent.CompletableFuture;

/**
 * Service chuyên biệt cho Job 2: Gửi email cảnh báo bảo mật tới người dùng và Đồng bộ đóng sự cố lên VNPT CTIP.
 */
public interface LeakNotificationService {

    /**
     * Thực thi Job 2 đồng bộ: Gửi email và đồng bộ đóng sự cố lên CTIP cho các tài khoản PENDING.
     *
     * @return Bản ghi JobHistory sau khi hoàn tất.
     */
    JobHistory executeJob2SendEmailsAndSyncCtip();

    /**
     * Thực thi Job 2 bất đồng bộ (@Async) cho JobHistory đã khởi tạo trước.
     *
     * @param job Bản ghi lịch sử Job.
     * @return CompletableFuture chứa JobHistory sau khi hoàn tất.
     */
    CompletableFuture<JobHistory> executeJob2Async(JobHistory job);
}
