package com.vnpt.leakprocessor.service.impl;

import com.vnpt.leakprocessor.model.JobHistory;
import com.vnpt.leakprocessor.service.LeakFetchService;
import com.vnpt.leakprocessor.service.LeakNotificationService;
import com.vnpt.leakprocessor.service.LeakProcessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service điều phối kết nối các tác vụ chuyên biệt của LeakFetchService và LeakNotificationService.
 */
@Service
public class LeakProcessorServiceImpl implements LeakProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(LeakProcessorServiceImpl.class);

    private final LeakFetchService leakFetchService;
    private final LeakNotificationService leakNotificationService;

    public LeakProcessorServiceImpl(
            LeakFetchService leakFetchService,
            LeakNotificationService leakNotificationService) {
        this.leakFetchService = leakFetchService;
        this.leakNotificationService = leakNotificationService;
    }

    // === JOB 1 ===
    @Override
    public JobHistory processJob1FetchAndVerify() {
        logger.info("LeakProcessorService: Dieu phoi thuc thi Job 1 (Fetch & SmartCA Verify)...");
        return leakFetchService.executeJob1FetchAndVerify();
    }

    @Override
    @Async
    public CompletableFuture<JobHistory> processJob1Async(JobHistory job) {
        logger.info("LeakProcessorService: Dieu phoi thuc thi Job 1 bat dong bo cho Job ID: {}", job.getId());
        return leakFetchService.executeJob1Async(job);
    }

    // === JOB 2 ===
    @Override
    public JobHistory processJob2SendEmailsAndSyncCtip() {
        logger.info("LeakProcessorService: Dieu phoi thuc thi Job 2 (Send Emails & CTIP Sync)...");
        return leakNotificationService.executeJob2SendEmailsAndSyncCtip();
    }

    @Override
    @Async
    public CompletableFuture<JobHistory> processJob2Async(JobHistory job) {
        logger.info("LeakProcessorService: Dieu phoi thuc thi Job 2 bat dong bo cho Job ID: {}", job.getId());
        return leakNotificationService.executeJob2Async(job);
    }
}
