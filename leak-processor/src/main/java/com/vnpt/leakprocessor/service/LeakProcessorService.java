package com.vnpt.leakprocessor.service;

import com.vnpt.leakprocessor.model.JobHistory;
import java.util.concurrent.CompletableFuture;

/**
 * Interface dịch vụ điều phối xử lý rò rỉ tài khoản.
 * Khởi tạo và điều hướng thực thi Job 1 (Quét & Xác thực SmartCA) và Job 2 (Gửi Email & Đóng CTIP).
 */
public interface LeakProcessorService {

    /**
     * Kích hoạt Job 1 đồng bộ: Quét CTIP API và xác thực qua SmartCA Gateway.
     */
    JobHistory processJob1FetchAndVerify();

    /**
     * Kích hoạt Job 1 bất đồng bộ (@Async).
     */
    CompletableFuture<JobHistory> processJob1Async(JobHistory job);

    /**
     * Kích hoạt Job 2 đồng bộ: Gửi email cảnh báo và đồng bộ đóng sự cố lên CTIP.
     */
    JobHistory processJob2SendEmailsAndSyncCtip();

    /**
     * Kích hoạt Job 2 bất đồng bộ (@Async).
     */
    CompletableFuture<JobHistory> processJob2Async(JobHistory job);
}
