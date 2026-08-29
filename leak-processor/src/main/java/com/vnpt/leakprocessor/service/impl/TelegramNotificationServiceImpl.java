package com.vnpt.leakprocessor.service.impl;

import com.vnpt.leakprocessor.model.JobHistory;
import com.vnpt.leakprocessor.service.TelegramNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service triển khai tích hợp cổng Telegram Bot HTTP API (sendMessage).
 * Hỗ trợ định dạng tin nhắn HTML và thực thi phát thông báo bất đồng bộ/try-catch an toàn.
 */
@Service
public class TelegramNotificationServiceImpl implements TelegramNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramNotificationServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate restTemplate;

    @Value("${telegram.bot.enabled:true}")
    private boolean enabled;

    @Value("${telegram.bot.token:}")
    private String botToken;

    @Value("${telegram.bot.chat-id:}")
    private String chatId;

    @Value("${ctip.api.domain-search:smartca.vnpt.vn}")
    private String domainSearch;

    public TelegramNotificationServiceImpl(
            @Qualifier("telegramRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Async
    public void sendJob1Report(JobHistory job, int authFailedCount) {
        if (!enabled || botToken.isBlank() || chatId.isBlank()) {
            logger.debug("TelegramNotificationService: Cong thong bao Telegram Bot chua duoc cau hinh hoac bi tat. Bo qua.");
            return;
        }

        String startTimeStr = (job.getStartTime() != null) ? job.getStartTime().format(DATE_FORMATTER) : "N/A";
        String endTimeStr = (job.getEndTime() != null) ? job.getEndTime().format(DATE_FORMATTER) : "N/A";

        StringBuilder sb = new StringBuilder();
        sb.append("<b>🔒 VNPT SMARTCA - CTIP SCAN REPORT</b>\n\n");
        sb.append("<b>📌 Domain:</b> ").append(domainSearch).append("\n");
        sb.append("<b>🆔 Job ID:</b> #").append(job.getId()).append(" (").append(job.getJobName()).append(")\n");
        sb.append("<b>🚦 Trạng thái:</b> ").append(job.getStatus()).append("\n\n");

        sb.append("<b>📊 Tổng số tài khoản quét từ CTIP:</b> ").append(job.getTotalFetched()).append("\n");
        sb.append("<b>✅ Xác thực SmartCA hợp lệ (Lưu PENDING):</b> ").append(job.getTotalMapped()).append("\n");
        sb.append("<b>❌ Xác thực SmartCA thất bại:</b> ").append(authFailedCount).append("\n\n");

        sb.append("<b>⏰ Bắt đầu:</b> ").append(startTimeStr).append("\n");
        sb.append("<b>⏰ Kết thúc:</b> ").append(endTimeStr);

        sendTelegramHtmlMessage(sb.toString());
    }

    @Override
    @Async
    public void sendJob2Report(JobHistory job, int emailFailedCount, int ctipFailedCount) {
        if (!enabled || botToken.isBlank() || chatId.isBlank()) {
            logger.debug("TelegramNotificationService: Cong thong bao Telegram Bot chua duoc cau hinh hoac bi tat. Bo qua.");
            return;
        }

        String startTimeStr = (job.getStartTime() != null) ? job.getStartTime().format(DATE_FORMATTER) : "N/A";
        String endTimeStr = (job.getEndTime() != null) ? job.getEndTime().format(DATE_FORMATTER) : "N/A";

        StringBuilder sb = new StringBuilder();
        sb.append("<b>✉️ VNPT SMARTCA - NOTIFICATION & CTIP CLOSE REPORT</b>\n\n");
        sb.append("<b>🆔 Job ID:</b> #").append(job.getId()).append(" (").append(job.getJobName()).append(")\n");
        sb.append("<b>🚦 Trạng thái:</b> ").append(job.getStatus()).append("\n\n");

        sb.append("<b>📧 Email cảnh báo đã gửi thành công:</b> ").append(job.getTotalSentEmail()).append("\n");
        sb.append("<b>🟢 Sự cố đã đồng bộ đóng trên CTIP:</b> ").append(job.getTotalUpdatedCtip()).append("\n\n");

        sb.append("<b>⚠️ Email bị lỗi gửi:</b> ").append(emailFailedCount).append("\n");
        sb.append("<b>⚠️ CTIP bị lỗi đồng bộ:</b> ").append(ctipFailedCount).append("\n\n");

        sb.append("<b>⏰ Bắt đầu:</b> ").append(startTimeStr).append("\n");
        sb.append("<b>⏰ Kết thúc:</b> ").append(endTimeStr);

        sendTelegramHtmlMessage(sb.toString());
    }

    @Override
    @Async
    public void sendJobFailureAlert(JobHistory job, String errorMessage) {
        if (!enabled || botToken.isBlank() || chatId.isBlank()) {
            return;
        }

        String timeStr = (job.getEndTime() != null) ? job.getEndTime().format(DATE_FORMATTER) : "N/A";

        StringBuilder sb = new StringBuilder();
        sb.append("<b>🚨 ALERT: JOB EXECUTION FAILED</b>\n\n");
        sb.append("<b>🆔 Job ID:</b> #").append(job.getId()).append(" (").append(job.getJobName()).append(")\n");
        sb.append("<b>💥 Chi tiết lỗi:</b> ").append(errorMessage != null ? errorMessage : "Không xác định").append("\n\n");
        sb.append("<b>⏰ Thời gian phát sinh:</b> ").append(timeStr);

        sendTelegramHtmlMessage(sb.toString());
    }

    /**
     * Phương thức nội bộ thực hiện gửi HTTP POST request tới Telegram Bot API.
     */
    private void sendTelegramHtmlMessage(String htmlMessage) {
        try {
            String telegramApiUrl = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", htmlMessage);
            body.put("parse_mode", "HTML");

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            logger.info("TelegramNotificationService: Dang gui thong bao Telegram Bot toi Chat ID '{}'...", chatId);
            restTemplate.postForEntity(telegramApiUrl, requestEntity, String.class);
            logger.info("TelegramNotificationService: Gui thong bao Telegram Bot thanh cong!");
        } catch (Exception e) {
            logger.error("TelegramNotificationService: Loi khi gui thong bao qua Telegram Bot API: {}", e.getMessage());
        }
    }
}
