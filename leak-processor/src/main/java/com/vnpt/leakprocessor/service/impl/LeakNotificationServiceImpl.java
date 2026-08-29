package com.vnpt.leakprocessor.service.impl;

import com.vnpt.leakprocessor.client.CtipClient;
import com.vnpt.leakprocessor.model.CredentialLeak;
import com.vnpt.leakprocessor.model.EmailTemplate;
import com.vnpt.leakprocessor.model.JobHistory;
import com.vnpt.leakprocessor.model.ProcessingLog;
import com.vnpt.leakprocessor.repository.CredentialLeakRepository;
import com.vnpt.leakprocessor.repository.EmailTemplateRepository;
import com.vnpt.leakprocessor.repository.JobHistoryRepository;
import com.vnpt.leakprocessor.repository.ProcessingLogRepository;
import com.vnpt.leakprocessor.service.EmailService;
import com.vnpt.leakprocessor.service.LeakNotificationService;
import com.vnpt.leakprocessor.service.TelegramNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service triển khai Job 2: Gửi email cảnh báo bảo mật tới người dùng và Đồng bộ đóng sự cố lên VNPT CTIP API.
 * Đã bọc cơ chế cô lập Transaction từng bước độc lập (REQUIRES_NEW) để ghi nhận chi tiết nhật ký lỗi.
 */
@Service
public class LeakNotificationServiceImpl implements LeakNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(LeakNotificationServiceImpl.class);

    private final EmailService emailService;
    private final CtipClient ctipClient;
    private final CredentialLeakRepository credentialLeakRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final JobHistoryRepository jobHistoryRepository;
    private final ProcessingLogRepository processingLogRepository;
    private final TelegramNotificationService telegramNotificationService;

    @Autowired
    @Lazy
    private LeakNotificationServiceImpl self;

    public LeakNotificationServiceImpl(
            EmailService emailService,
            CtipClient ctipClient,
            CredentialLeakRepository credentialLeakRepository,
            EmailTemplateRepository emailTemplateRepository,
            JobHistoryRepository jobHistoryRepository,
            ProcessingLogRepository processingLogRepository,
            TelegramNotificationService telegramNotificationService) {
        this.emailService = emailService;
        this.ctipClient = ctipClient;
        this.credentialLeakRepository = credentialLeakRepository;
        this.emailTemplateRepository = emailTemplateRepository;
        this.jobHistoryRepository = jobHistoryRepository;
        this.processingLogRepository = processingLogRepository;
        this.telegramNotificationService = telegramNotificationService;
    }

    /**
     * Thực thi Job 2 đồng bộ: Gửi Email & Đóng sự cố CTIP với cơ chế tách bước log lỗi độc lập.
     */
    @Override
    public JobHistory executeJob2SendEmailsAndSyncCtip() {
        JobHistory job = new JobHistory();
        job.setJobName("JOB2_EMAIL_AND_CTIP_SYNC");
        job.setStartTime(LocalDateTime.now());
        job.setStatus("RUNNING");
        job.setTotalFetched(0);
        job.setTotalMapped(0);
        job.setTotalSentEmail(0);
        job.setTotalUpdatedCtip(0);
        job = jobHistoryRepository.save(job);

        logger.info("LeakNotificationService: Bat dau thuc thi Job 2 (Send Email & CTIP Sync) voi Job ID: {}", job.getId());

        try {
            int[] failedCounts = processPendingAndFailedLeaks(job);
            job.setStatus("SUCCESS");
            job.setEndTime(LocalDateTime.now());
            logger.info("LeakNotificationService: Job 2 hoan tat thanh cong cho Job ID: {}", job.getId());

            JobHistory savedJob = jobHistoryRepository.save(job);
            telegramNotificationService.sendJob2Report(savedJob, failedCounts[0], failedCounts[1]);
            return savedJob;
        } catch (Exception e) {
            logger.error("LeakNotificationService: Job 2 gap loi khi thuc thi Job ID: {}. Chi tiet: {}", job.getId(), e.getMessage(), e);
            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
            job.setEndTime(LocalDateTime.now());

            JobHistory savedJob = jobHistoryRepository.save(job);
            telegramNotificationService.sendJobFailureAlert(savedJob, e.getMessage());
            return savedJob;
        }
    }

    /**
     * Thực thi Job 2 bất đồng bộ (@Async) cho JobHistory đã khởi tạo trước.
     */
    @Override
    @Async
    public CompletableFuture<JobHistory> executeJob2Async(JobHistory job) {
        logger.info("LeakNotificationService: Kich hoat luong chay bat dong bo Job 2 cho Job ID: {}", job.getId());
        try {
            int[] failedCounts = processPendingAndFailedLeaks(job);
            job.setStatus("SUCCESS");
            job.setEndTime(LocalDateTime.now());
            logger.info("LeakNotificationService: Luong bat dong bo Job 2 hoan tat cho Job ID: {}", job.getId());

            JobHistory updatedJob = jobHistoryRepository.save(job);
            telegramNotificationService.sendJob2Report(updatedJob, failedCounts[0], failedCounts[1]);
            return CompletableFuture.completedFuture(updatedJob);
        } catch (Exception e) {
            logger.error("LeakNotificationService: Luong bat dong bo Job 2 gap loi cho Job ID: {}. Chi tiet: {}", job.getId(), e.getMessage(), e);
            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
            job.setEndTime(LocalDateTime.now());

            JobHistory updatedJob = jobHistoryRepository.save(job);
            telegramNotificationService.sendJobFailureAlert(updatedJob, e.getMessage());
            return CompletableFuture.completedFuture(updatedJob);
        }
    }

    /**
     * Duyệt qua toàn bộ danh sách các tài khoản PENDING / EMAIL_FAILED (cần gửi mail + đóng CTIP)
     * và các tài khoản CTIP_UPDATE_FAILED (chỉ retry đồng bộ CTIP).
     * Trả về mảng int [emailFailedCount, ctipFailedCount].
     */
    private int[] processPendingAndFailedLeaks(JobHistory job) {
        List<CredentialLeak> pendingLeaks = credentialLeakRepository.findByLocalStatusIn(List.of("PENDING", "EMAIL_FAILED"));
        List<CredentialLeak> ctipFailedLeaks = credentialLeakRepository.findByLocalStatus("CTIP_UPDATE_FAILED");

        if (pendingLeaks.isEmpty() && ctipFailedLeaks.isEmpty()) {
            logger.info("LeakNotificationService: Khong co ban ghi nao can xu ly trong Job 2.");
            return new int[]{0, 0};
        }

        EmailTemplate template = emailTemplateRepository.findByTemplateName("smartca-warning")
                .orElseGet(() -> {
                    EmailTemplate fallback = new EmailTemplate();
                    fallback.setSubject("CẢNH BÁO BẢO MẬT: Tài khoản VNPT SmartCA của bạn có nguy cơ bị lộ lọt");
                    fallback.setBodyHtml("<h3>Kính gửi Quý khách hàng,</h3><p>Hệ thống VNPT ghi nhận tài khoản SmartCA của bạn có dấu hiệu lộ lọt thông tin. Vui lòng đổi mật khẩu ngay lập tức.</p>");
                    return fallback;
                });

        int sentCount = 0;
        int closedCount = 0;

        // 1. Xử lý các tài khoản PENDING (Bước 1: Gửi mail -> Bước 2: Đồng bộ CTIP)
        for (CredentialLeak leak : pendingLeaks) {
            if (leak.getEmail() == null || leak.getEmail().isBlank()) {
                logger.warn("LeakNotificationService: Leak ID {} khong co dia chi email. Bo qua.", leak.getId());
                continue;
            }

            // Bước 1: Gửi email cảnh báo
            boolean emailOk = self.sendEmailForLeakStep(leak.getId(), template, job);
            if (emailOk) {
                sentCount++;
                // Bước 2: Đồng bộ đóng sự cố lên CTIP ngay lập tức
                boolean syncOk = self.syncCtipForLeakStep(leak.getId(), job);
                if (syncOk) {
                    closedCount++;
                }
            }
        }

        // 2. Retry cho các tài khoản đã gửi mail thành công trước đó nhưng từng bị lỗi đồng bộ CTIP
        for (CredentialLeak leak : ctipFailedLeaks) {
            boolean syncOk = self.syncCtipForLeakStep(leak.getId(), job);
            if (syncOk) {
                closedCount++;
            }
        }

        if (job != null) {
            job.setTotalSentEmail(job.getTotalSentEmail() + sentCount);
            job.setTotalUpdatedCtip(job.getTotalUpdatedCtip() + closedCount);
            jobHistoryRepository.save(job);
        }

        long finalEmailFailedCount = credentialLeakRepository.countByLocalStatus("EMAIL_FAILED");
        long finalCtipFailedCount = credentialLeakRepository.countByLocalStatus("CTIP_UPDATE_FAILED");

        return new int[]{(int) finalEmailFailedCount, (int) finalCtipFailedCount};
    }

    /**
     * BƯỚC 1: Gửi email cảnh báo bảo mật cho 1 tài khoản trong Transaction độc lập (Propagation.REQUIRES_NEW).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean sendEmailForLeakStep(Long leakId, EmailTemplate template, JobHistory job) {
        CredentialLeak leak = credentialLeakRepository.findById(leakId).orElse(null);
        if (leak == null) return false;

        try {
            emailService.sendWarningEmail(leak.getEmail(), template.getSubject(), template.getBodyHtml());

            leak.setLocalStatus("EMAIL_SENT");
            credentialLeakRepository.save(leak);

            ProcessingLog log = new ProcessingLog();
            log.setJob(job);
            log.setLeak(leak);
            log.setStep("SEND_EMAIL");
            log.setStatus("SUCCESS");
            log.setMessage("Gửi email cảnh báo thành công tới hòm thư: " + leak.getEmail());
            log.setCreatedAt(LocalDateTime.now());
            processingLogRepository.save(log);

            return true;
        } catch (Exception e) {
            logger.error("LeakNotificationService: Gui email failure cho Leak ID {}: {}", leak.getId(), e.getMessage());

            leak.setLocalStatus("EMAIL_FAILED");
            credentialLeakRepository.save(leak);

            ProcessingLog log = new ProcessingLog();
            log.setJob(job);
            log.setLeak(leak);
            log.setStep("SEND_EMAIL");
            log.setStatus("FAILED");

            String stackTrace = getStackTraceAsString(e);
            log.setMessage("Gửi email cảnh báo thất bại tới hòm thư: " + leak.getEmail() + ". Lý do: " + e.getMessage());
            log.setStackTrace(stackTrace);
            log.setCreatedAt(LocalDateTime.now());
            processingLogRepository.save(log);

            return false;
        }
    }

    /**
     * BƯỚC 2: Đồng bộ đóng sự cố lên VNPT CTIP API cho 1 tài khoản trong Transaction độc lập (Propagation.REQUIRES_NEW).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean syncCtipForLeakStep(Long leakId, JobHistory job) {
        CredentialLeak leak = credentialLeakRepository.findById(leakId).orElse(null);
        if (leak == null) return false;

        try {
            String statusIdToUpdate = leak.getStatusId() != null ? leak.getStatusId() : leak.getCredentialId();
            ctipClient.updateCtipStatus(List.of(statusIdToUpdate), "close");

            leak.setLocalStatus("PROCESSED");
            leak.setCtipStatus("close");
            credentialLeakRepository.save(leak);

            ProcessingLog log = new ProcessingLog();
            log.setJob(job);
            log.setLeak(leak);
            log.setStep("SYNC_CTIP");
            log.setStatus("SUCCESS");
            log.setMessage("Đồng bộ đóng sự cố thành công lên CTIP cho Credential ID: " + leak.getCredentialId());
            log.setCreatedAt(LocalDateTime.now());
            processingLogRepository.save(log);

            return true;
        } catch (Exception e) {
            logger.error("LeakNotificationService: Dong bo CTIP failure cho Leak ID {}: {}", leak.getId(), e.getMessage());

            leak.setLocalStatus("CTIP_UPDATE_FAILED");
            credentialLeakRepository.save(leak);

            ProcessingLog log = new ProcessingLog();
            log.setJob(job);
            log.setLeak(leak);
            log.setStep("SYNC_CTIP");
            log.setStatus("FAILED");

            String stackTrace = getStackTraceAsString(e);
            log.setMessage("Đồng bộ đóng sự cố lên CTIP thất bại cho Credential ID: " + leak.getCredentialId() + ". Lý do: " + e.getMessage());
            log.setStackTrace(stackTrace);
            log.setCreatedAt(LocalDateTime.now());
            processingLogRepository.save(log);

            return false;
        }
    }

    private String getStackTraceAsString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
