package com.vnpt.leakprocessor.service.impl;

import com.vnpt.leakprocessor.model.CredentialLeak;
import com.vnpt.leakprocessor.model.JobHistory;
import com.vnpt.leakprocessor.model.ProcessingLog;
import com.vnpt.leakprocessor.repository.CredentialLeakRepository;
import com.vnpt.leakprocessor.repository.JobHistoryRepository;
import com.vnpt.leakprocessor.repository.ProcessingLogRepository;
import com.vnpt.leakprocessor.service.DashboardService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service triển khai xử lý truy vấn dữ liệu thống kê Dashboard và chi tiết các phiên chạy Job.
 */
@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final CredentialLeakRepository credentialLeakRepository;
    private final JobHistoryRepository jobHistoryRepository;
    private final ProcessingLogRepository processingLogRepository;

    public DashboardServiceImpl(
            CredentialLeakRepository credentialLeakRepository,
            JobHistoryRepository jobHistoryRepository,
            ProcessingLogRepository processingLogRepository) {
        this.credentialLeakRepository = credentialLeakRepository;
        this.jobHistoryRepository = jobHistoryRepository;
        this.processingLogRepository = processingLogRepository;
    }

    @Override
    public Map<String, Object> getDashboardStatsMap() {
        Map<String, Object> stats = new HashMap<>();

        long totalLeaks = credentialLeakRepository.count();
        long totalPending = credentialLeakRepository.countByLocalStatus("PENDING");
        long totalEmailed = credentialLeakRepository.countByLocalStatus("EMAIL_SENT");
        long totalProcessed = credentialLeakRepository.countByLocalStatus("PROCESSED");
        long totalFailed = credentialLeakRepository.countByLocalStatus("EMAIL_FAILED");
        long totalCtipUpdateFailed = credentialLeakRepository.countByLocalStatus("CTIP_UPDATE_FAILED");

        long severityLow = credentialLeakRepository.countBySeverity("low");
        long severityMedium = credentialLeakRepository.countBySeverity("medium");
        long severityHigh = credentialLeakRepository.countBySeverity("high");
        long severityCritical = credentialLeakRepository.countBySeverity("critical");

        List<CredentialLeak> latestLeaks = credentialLeakRepository.findAll(
                PageRequest.of(0, 5, Sort.by("id").descending())).getContent();

        List<Map<String, Object>> leaksList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (CredentialLeak leak : latestLeaks) {
            Map<String, Object> leakMap = new HashMap<>();
            leakMap.put("credentialId", leak.getCredentialId());
            leakMap.put("username", leak.getUsername());
            leakMap.put("severity", leak.getSeverity());
            leakMap.put("email", leak.getEmail() != null ? leak.getEmail() : "N/A");
            leakMap.put("phone", leak.getPhone() != null ? leak.getPhone() : "N/A");
            leakMap.put("localStatus", leak.getLocalStatus());
            leakMap.put("compromiseTime",
                    leak.getCompromiseTime() != null ? leak.getCompromiseTime().format(formatter) : "N/A");
            leaksList.add(leakMap);
        }

        stats.put("totalLeaks", totalLeaks);
        stats.put("totalPending", totalPending);
        stats.put("totalEmailed", totalEmailed);
        stats.put("totalProcessed", totalProcessed);
        stats.put("totalFailed", totalFailed + totalCtipUpdateFailed);

        stats.put("severityLow", severityLow);
        stats.put("severityMedium", severityMedium);
        stats.put("severityHigh", severityHigh);
        stats.put("severityCritical", severityCritical);

        stats.put("latestLeaks", leaksList);
        stats.put("rawLatestLeaks", latestLeaks);

        return stats;
    }

    @Override
    public List<CredentialLeak> getAllLeaks() {
        return credentialLeakRepository.findAll(Sort.by("id").descending());
    }

    @Override
    public List<JobHistory> getAllJobs() {
        return jobHistoryRepository.findAll(Sort.by("id").descending());
    }

    @Override
    public JobHistory getJobById(Long id) {
        return jobHistoryRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public JobHistory createAndSaveJobHistory(String jobName) {
        JobHistory job = new JobHistory();
        job.setJobName(jobName);
        job.setStartTime(LocalDateTime.now());
        job.setStatus("RUNNING");
        job.setTotalFetched(0);
        job.setTotalMapped(0);
        job.setTotalSentEmail(0);
        job.setTotalUpdatedCtip(0);
        return jobHistoryRepository.save(job);
    }

    @Override
    public List<CredentialLeak> getLeaksByJobId(Long jobId) {
        return credentialLeakRepository.findByJobId(jobId);
    }

    @Override
    public List<ProcessingLog> getLogsByJobId(Long jobId) {
        return processingLogRepository.findByJobId(jobId);
    }
}
