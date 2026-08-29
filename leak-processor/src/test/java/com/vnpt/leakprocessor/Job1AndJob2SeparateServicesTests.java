package com.vnpt.leakprocessor;

import com.vnpt.leakprocessor.model.CredentialLeak;
import com.vnpt.leakprocessor.model.JobHistory;
import com.vnpt.leakprocessor.model.ProcessingLog;
import com.vnpt.leakprocessor.repository.CredentialLeakRepository;
import com.vnpt.leakprocessor.repository.JobHistoryRepository;
import com.vnpt.leakprocessor.repository.ProcessingLogRepository;
import com.vnpt.leakprocessor.service.LeakFetchService;
import com.vnpt.leakprocessor.service.LeakNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;
import jakarta.mail.internet.MimeMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestConfig.class)
public class Job1AndJob2SeparateServicesTests {

    @Autowired
    private LeakFetchService leakFetchService;

    @Autowired
    private LeakNotificationService leakNotificationService;

    @Autowired
    private CredentialLeakRepository credentialLeakRepository;

    @Autowired
    private JobHistoryRepository jobHistoryRepository;

    @Autowired
    private ProcessingLogRepository processingLogRepository;

    @MockBean
    private JavaMailSender mailSender;

    @BeforeEach
    public void setUp() {
        processingLogRepository.deleteAll();
        credentialLeakRepository.deleteAll();
        jobHistoryRepository.deleteAll();

        MimeMessage dummyMimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(dummyMimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));
    }

    @Test
    public void testJob1FetchAndVerify_ExecutesJob1Only() {
        JobHistory job1 = leakFetchService.executeJob1FetchAndVerify();
        assertNotNull(job1);
        assertEquals("SUCCESS", job1.getStatus());

        List<CredentialLeak> savedLeaks = credentialLeakRepository.findAll();
        assertFalse(savedLeaks.isEmpty(), "Job 1 should save leaks with PENDING status to DB");

        for (CredentialLeak leak : savedLeaks) {
            assertEquals("PENDING", leak.getLocalStatus(), "Leaks saved by Job 1 must have status PENDING");
            assertNotNull(leak.getEmail(), "Leak email must be extracted from SmartCA");
        }

        // Verify invalid auth record was NOT saved in credential_leaks but recorded in processing_logs
        boolean invalidLeakSaved = credentialLeakRepository.existsByCredentialId("019adf91-invalid-auth-test-9999");
        assertFalse(invalidLeakSaved, "Leak with invalid SmartCA password should NOT be saved in DB");

        List<ProcessingLog> failedLogs = processingLogRepository.findByJobId(job1.getId()).stream()
                .filter(l -> "FAILED".equals(l.getStatus()))
                .toList();
        assertFalse(failedLogs.isEmpty(), "ProcessingLogs should contain failed auth record for invalid user");
    }

    @Test
    public void testJob2SendEmailsAndSync_ExecutesJob2Only() {
        // Prepare Job 1 data first
        leakFetchService.executeJob1FetchAndVerify();

        // Run Job 2 independently
        JobHistory job2 = leakNotificationService.executeJob2SendEmailsAndSyncCtip();
        assertNotNull(job2);
        assertEquals("SUCCESS", job2.getStatus());

        List<CredentialLeak> processedLeaks = credentialLeakRepository.findAll();
        for (CredentialLeak leak : processedLeaks) {
            assertEquals("PROCESSED", leak.getLocalStatus(), "Job 2 should update all PENDING leaks to PROCESSED");
            assertEquals("close", leak.getCtipStatus(), "Job 2 should set ctipStatus to close");
        }
    }
}
