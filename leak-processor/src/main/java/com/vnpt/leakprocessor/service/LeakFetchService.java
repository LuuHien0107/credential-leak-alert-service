package com.vnpt.leakprocessor.service;

import com.vnpt.leakprocessor.model.JobHistory;
import java.util.concurrent.CompletableFuture;

/**
 * Service chuyên biệt cho Job 1: Quét dữ liệu rò rỉ từ CTIP API và Xác thực & làm giàu dữ liệu qua VNPT SmartCA Gateway.
 */
public interface LeakFetchService {

    /**
     * Thực thi Job 1 đồng bộ: Quét CTIP, xác thực tài khoản SmartCA và lưu các bản ghi hợp lệ vào DB dưới trạng thái PENDING.
     *
     * @return Bản ghi JobHistory sau khi hoàn tất.
     */
    JobHistory executeJob1FetchAndVerify();

    /**
     * Thực thi Job 1 bất đồng bộ (@Async) cho JobHistory đã khởi tạo trước.
     *
     * @param job Bản ghi lịch sử Job.
     * @return CompletableFuture chứa JobHistory sau khi hoàn tất.
     */
    CompletableFuture<JobHistory> executeJob1Async(JobHistory job);

    /**
     * Quét và đối soát dữ liệu lộ lọt CTIP trong ngữ cảnh của Job hiện tại.
     *
     * @param job Bản ghi lịch sử Job đang chạy.
     */
    void executeScan(JobHistory job);
}
