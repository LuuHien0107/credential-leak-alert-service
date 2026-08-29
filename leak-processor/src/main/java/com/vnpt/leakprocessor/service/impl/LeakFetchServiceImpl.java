package com.vnpt.leakprocessor.service.impl;

import com.vnpt.leakprocessor.client.CtipClient;
import com.vnpt.leakprocessor.client.SmartCaClient;
import com.vnpt.leakprocessor.dto.CtipLeakResponse;
import com.vnpt.leakprocessor.dto.CtipLeakResult;
import com.vnpt.leakprocessor.dto.SmartCaUserInfoResponse;
import com.vnpt.leakprocessor.model.CredentialLeak;
import com.vnpt.leakprocessor.model.JobHistory;
import com.vnpt.leakprocessor.model.ProcessingLog;
import com.vnpt.leakprocessor.repository.CredentialLeakRepository;
import com.vnpt.leakprocessor.repository.JobHistoryRepository;
import com.vnpt.leakprocessor.repository.ProcessingLogRepository;
import com.vnpt.leakprocessor.service.LeakFetchService;
import com.vnpt.leakprocessor.service.TelegramNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Service triển khai Job 1: Lấy thông tin lộ lọt từ CTIP, xác thực tài khoản qua VNPT SmartCA Gateway và lưu CSDL.
 */
@Service
public class LeakFetchServiceImpl implements LeakFetchService {

    private static final Logger logger = LoggerFactory.getLogger(LeakFetchServiceImpl.class);

    private final CtipClient ctipClient;
    private final SmartCaClient smartCaClient;
    private final CredentialLeakRepository credentialLeakRepository;
    private final JobHistoryRepository jobHistoryRepository;
    private final ProcessingLogRepository processingLogRepository;
    private final TelegramNotificationService telegramNotificationService;

    @Value("${ctip.api.domain-search:smartca.vnpt.vn}")
    private String domainSearch;

    @Value("${job1.scan.days-back:0}")
    private int daysBack;

    @Value("${ctip.api.page-size:100}")
    private int pageSize;

    public LeakFetchServiceImpl(
            CtipClient ctipClient,
            SmartCaClient smartCaClient,
            CredentialLeakRepository credentialLeakRepository,
            JobHistoryRepository jobHistoryRepository,
            ProcessingLogRepository processingLogRepository,
            TelegramNotificationService telegramNotificationService) {
        this.ctipClient = ctipClient;
        this.smartCaClient = smartCaClient;
        this.credentialLeakRepository = credentialLeakRepository;
        this.jobHistoryRepository = jobHistoryRepository;
        this.processingLogRepository = processingLogRepository;
        this.telegramNotificationService = telegramNotificationService;
    }

    /**
     * Thực thi Job 1 đồng bộ: Tạo bản ghi JobHistory và quét xác thực dữ liệu rò rỉ.
     */
    @Override
    @Transactional
    public JobHistory executeJob1FetchAndVerify() {
        JobHistory job = new JobHistory();
        job.setJobName("JOB1_FETCH_AND_VERIFY");
        job.setStartTime(LocalDateTime.now());
        job.setStatus("RUNNING");
        job.setTotalFetched(0);
        job.setTotalMapped(0);
        job.setTotalSentEmail(0);
        job.setTotalUpdatedCtip(0);
        job = jobHistoryRepository.save(job);

        logger.info("LeakFetchService: Bat dau thuc thi Job 1 (Scan CTIP & Auth SmartCA) voi Job ID: {}", job.getId());

        try {
            int authFailedCount = executeScanWithDetails(job);
            job.setStatus("SUCCESS");
            job.setEndTime(LocalDateTime.now());
            logger.info("LeakFetchService: Job 1 hoan tat thanh cong cho Job ID: {}", job.getId());

            JobHistory savedJob = jobHistoryRepository.save(job);
            telegramNotificationService.sendJob1Report(savedJob, authFailedCount);
            return savedJob;
        } catch (Exception e) {
            logger.error("LeakFetchService: Job 1 gap loi khi thuc thi Job ID: {}. Chi tiet: {}", job.getId(), e.getMessage(), e);
            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
            job.setEndTime(LocalDateTime.now());

            JobHistory savedJob = jobHistoryRepository.save(job);
            telegramNotificationService.sendJobFailureAlert(savedJob, e.getMessage());
            return savedJob;
        }
    }

    /**
     * Thực thi Job 1 bất đồng bộ (@Async) cho JobHistory đã khởi tạo trước.
     */
    @Override
    @Async
    public CompletableFuture<JobHistory> executeJob1Async(JobHistory job) {
        logger.info("LeakFetchService: Kich hoat luong chay bat dong bo Job 1 cho Job ID: {}", job.getId());
        try {
            int authFailedCount = executeScanWithDetails(job);
            job.setStatus("SUCCESS");
            job.setEndTime(LocalDateTime.now());
            logger.info("LeakFetchService: Luong bat dong bo Job 1 hoan tat cho Job ID: {}", job.getId());

            JobHistory updatedJob = jobHistoryRepository.save(job);
            telegramNotificationService.sendJob1Report(updatedJob, authFailedCount);
            return CompletableFuture.completedFuture(updatedJob);
        } catch (Exception e) {
            logger.error("LeakFetchService: Luong bat dong bo Job 1 gap loi cho Job ID: {}. Chi tiet: {}", job.getId(), e.getMessage(), e);
            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
            job.setEndTime(LocalDateTime.now());

            JobHistory updatedJob = jobHistoryRepository.save(job);
            telegramNotificationService.sendJobFailureAlert(updatedJob, e.getMessage());
            return CompletableFuture.completedFuture(updatedJob);
        }
    }

    /**
     * Quét và đối soát dữ liệu lộ lọt CTIP trong ngữ cảnh của Job hiện tại.
     * Trả về số lượng tài khoản xác thực SmartCA thất bại.
     */
    @Override
    public void executeScan(JobHistory job) {
        executeScanWithDetails(job);
    }

    public int executeScanWithDetails(JobHistory job) {
        LocalDate lte = LocalDate.now();
        LocalDate gte = (daysBack > 0) ? lte.minusDays(daysBack) : null;
        int page = 1;
        int totalFetched = 0;
        int totalMapped = 0;
        int authFailedCount = 0;

        while (true) {
            logger.info("LeakFetchService: Dang tai du lieu CTIP trang {} (domainSearch='{}', daysBack={})...", page, domainSearch, daysBack);
            CtipLeakResponse response = ctipClient.fetchLeaks(domainSearch, gte, lte, page, pageSize);

            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                logger.info("LeakFetchService: Khong co du lieu ro ri o trang {}. Ket thuc quet.", page);
                break;
            }

            for (CtipLeakResult result : response.getResults()) {
                totalFetched++;

                // Lọc trùng theo credentialId
                if (credentialLeakRepository.existsByCredentialId(result.getCredentialId())) {
                    logger.debug("LeakFetchService: Credential ID '{}' da ton tai trong CSDL. Bo qua.", result.getCredentialId());
                    continue;
                }

                // Thực hiện đăng nhập qua SmartCA Auth Gateway
                String token = smartCaClient.loginAndGetToken(result.getUsername(), result.getPassword());

                // Nếu Auth thất bại: KHÔNG lưu bản ghi credential_leaks, lưu ProcessingLog báo lỗi
                if (token == null) {
                    authFailedCount++;
                    logger.warn("LeakFetchService: Xac thuc SmartCA THAT BAI cho tài khoản '{}'. Khong luu vao DB.", result.getUsername());

                    ProcessingLog failLog = new ProcessingLog();
                    failLog.setJob(job);
                    failLog.setStep("AUTH_SMARTCA");
                    failLog.setStatus("FAILED");
                    failLog.setMessage("Đăng nhập VNPT SmartCA thất bại cho tài khoản: " + result.getUsername());
                    failLog.setCreatedAt(LocalDateTime.now());
                    processingLogRepository.save(failLog);
                    continue;
                }

                // Nếu Auth thành công: Gọi API UserInfo lấy email & phone
                SmartCaUserInfoResponse userInfo = smartCaClient.getUserInfo(token);
                String email = null;
                String phone = null;
                if (userInfo != null && userInfo.getContent() != null) {
                    email = userInfo.getContent().getEmail();
                    phone = userInfo.getContent().getPhone();
                }

                // Lưu bản ghi vào bảng credential_leaks với localStatus = PENDING
                CredentialLeak leak = new CredentialLeak();
                leak.setJob(job);
                leak.setCredentialId(result.getCredentialId());
                leak.setStatusId(result.getStatusId());
                leak.setUsername(result.getUsername());
                leak.setPasswordEncrypted(result.getPassword());
                leak.setSeverity(result.getSeverity());
                leak.setEmail(email);
                leak.setPhone(phone);
                leak.setLeakUrl(result.getUrl());
                leak.setLocalStatus("PENDING");
                leak.setCtipStatus("open");

                if (result.getFirstCompromiseTime() != null) {
                    try {
                        leak.setCompromiseTime(OffsetDateTime.parse(result.getFirstCompromiseTime()).toLocalDateTime());
                    } catch (Exception ex) {
                        leak.setCompromiseTime(LocalDateTime.now());
                    }
                }
                if (result.getCreatedAt() != null) {
                    try {
                        leak.setCtipCreatedAt(OffsetDateTime.parse(result.getCreatedAt()).toLocalDateTime());
                    } catch (Exception ex) {
                        leak.setCtipCreatedAt(LocalDateTime.now());
                    }
                } else {
                    leak.setCtipCreatedAt(LocalDateTime.now());
                }

                credentialLeakRepository.save(leak);
                totalMapped++;

                // Ghi vết log thành công cho bước FETCH_AND_AUTH
                ProcessingLog successLog = new ProcessingLog();
                successLog.setJob(job);
                successLog.setLeak(leak);
                successLog.setStep("FETCH_AND_AUTH");
                successLog.setStatus("SUCCESS");
                successLog.setMessage("Xác thực SmartCA và trích xuất thành công Email: " + email + ", SĐT: " + phone);
                successLog.setCreatedAt(LocalDateTime.now());
                processingLogRepository.save(successLog);
            }

            if (response.getResults().size() < pageSize) {
                break;
            }

            page++;
        }

        if (job != null) {
            job.setTotalFetched(totalFetched);
            job.setTotalMapped(totalMapped);
            jobHistoryRepository.save(job);
        }

        return authFailedCount;
    }
}
