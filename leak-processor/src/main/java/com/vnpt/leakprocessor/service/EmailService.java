package com.vnpt.leakprocessor.service;

/**
 * Interface dịch vụ xử lý gửi email.
 */
public interface EmailService {

    /**
     * Gửi email HTML cảnh báo bảo mật tới khách hàng.
     *
     * @param toEmail Địa chỉ email người nhận.
     * @param subject Tiêu đề của thư cảnh báo.
     * @param bodyHtml Nội dung mã HTML của mẫu thư.
     */
    void sendWarningEmail(String toEmail, String subject, String bodyHtml);
}
