package com.vnpt.leakprocessor.service;

import com.vnpt.leakprocessor.model.JobHistory;

/**
 * Interface dịch vụ đẩy tin nhắn thông báo tự động qua Telegram Bot.
 */
public interface TelegramNotificationService {

    /**
     * Gửi bản tin báo cáo kết quả thực thi Job 1 (Quét CTIP & Đối soát SmartCA).
     *
     * @param job             đối tượng JobHistory lưu trữ kết quả chạy Job 1.
     * @param authFailedCount số lượng tài khoản xác thực SmartCA thất bại.
     */
    void sendJob1Report(JobHistory job, int authFailedCount);

    /**
     * Gửi bản tin báo cáo kết quả thực thi Job 2 (Gửi Email Cảnh báo & Đóng CTIP).
     *
     * @param job              đối tượng JobHistory lưu trữ kết quả chạy Job 2.
     * @param emailFailedCount số lượng email gửi thất bại.
     * @param ctipFailedCount  số lượng sự cố lỗi đồng bộ CTIP.
     */
    void sendJob2Report(JobHistory job, int emailFailedCount, int ctipFailedCount);

    /**
     * Gửi bản tin cảnh báo sự cố khẩn cấp khi tiến trình Job bị crash/lỗi ngắt đột ngột.
     *
     * @param job          đối tượng JobHistory chứa thông tin Job bị lỗi.
     * @param errorMessage thông điệp chi tiết về nguyên nhân gây lỗi.
     */
    void sendJobFailureAlert(JobHistory job, String errorMessage);
}
