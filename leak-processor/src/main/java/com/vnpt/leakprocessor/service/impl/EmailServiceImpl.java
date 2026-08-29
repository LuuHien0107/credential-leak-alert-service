package com.vnpt.leakprocessor.service.impl;

import com.vnpt.leakprocessor.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Lớp triển khai dịch vụ gửi Email thông báo.
 * Tích hợp cơ chế tự động thử lại (Spring Retry) khi gặp sự cố đường truyền
 * SMTP.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${spring.mail.username}")
    private String senderEmail;

    /**
     * Gửi email cảnh báo bảo mật tới địa chỉ email người nhận.
     * Cấu hình @Retryable: Thử lại tối đa 4 lần (1 lần đầu + 3 lần thử lại) nếu có
     * Exception.
     * Độ trễ tăng dần sau mỗi lần lỗi: lần 1 chờ 2 giây, lần sau nhân đôi độ trễ.
     */
    @Override
    @Retryable(retryFor = { Exception.class }, maxAttempts = 4, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void sendWarningEmail(String toEmail, String subject, String bodyHtml) {
        logger.info("Email Service: Bat dau gui email canh bao bao mat toi '{}'...", toEmail);

        try {
            MimeMessage message = mailSender.createMimeMessage();

            // Thiết lập helper với mã hóa UTF-8 để hỗ trợ tiếng Việt có dấu
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setFrom(senderEmail, "Hệ thống Cảnh báo VNPT SmartCA");
            helper.setSubject(subject);
            helper.setText(bodyHtml, true); // Thiết lập chế độ gửi HTML

            mailSender.send(message);
            logger.info("Email Service: Gui email canh bao bao mat thanh cong toi '{}'!", toEmail);

        } catch (Exception e) {
            logger.warn("Email Service: Loi tam thoi khi gui email toi '{}'. He thong se tu dong thu lai...", toEmail);
            throw new RuntimeException("Gui email canh bao qua giao thuc SMTP that bai", e);
        }
    }

    /**
     * Phương thức dự phòng (Recovery) tự động kích hoạt khi toàn bộ các lần thử lại
     * gửi mail đều thất bại.
     */
    @Recover
    public void recoverEmailFailure(Exception e, String toEmail, String subject, String bodyHtml) {
        logger.error("Email Service: Da thu lai toi da 4 lan gui thu toi '{}' nhung deu that bai. Loi cuoi cung: {}",
                toEmail, e.getMessage(), e);
        throw new RuntimeException(
                "Tien trinh gui email that bai vinh vien sau 3 lan gui (failed permanently after 3 retries). Chi tiet loi: "
                        + e.getMessage(),
                e);
    }
}
