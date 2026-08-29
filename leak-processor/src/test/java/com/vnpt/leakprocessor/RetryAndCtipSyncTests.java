package com.vnpt.leakprocessor;

import com.vnpt.leakprocessor.client.CtipClient;
import com.vnpt.leakprocessor.model.CredentialLeak;
import com.vnpt.leakprocessor.model.JobHistory;
import com.vnpt.leakprocessor.repository.CredentialLeakRepository;
import com.vnpt.leakprocessor.repository.JobHistoryRepository;
import com.vnpt.leakprocessor.repository.ProcessingLogRepository;
import com.vnpt.leakprocessor.service.EmailService;
import com.vnpt.leakprocessor.service.LeakProcessorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import jakarta.mail.internet.MimeMessage;

import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestConfig.class)
public class RetryAndCtipSyncTests {

    @Autowired
    private LeakProcessorService leakProcessorService;

    @Autowired
    private CredentialLeakRepository credentialLeakRepository;

    @Autowired
    private JobHistoryRepository jobHistoryRepository;

    @Autowired
    private ProcessingLogRepository processingLogRepository;

    @SpyBean
    private EmailService emailService;

    @SpyBean
    private CtipClient ctipClient;

    @MockBean
    private JavaMailSender mailSender;

    @BeforeEach
    public void setUp() {
        processingLogRepository.deleteAll();
        credentialLeakRepository.deleteAll();
        jobHistoryRepository.deleteAll();

        MimeMessage dummyMimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(dummyMimeMessage);
    }

    @Test
    public void testEmailServiceRetry_TransientFailureThenSuccess() {
        doThrow(new MailSendException("Transient SMTP Connection Error"))
                .doNothing()
                .when(mailSender).send(any(MimeMessage.class));

        emailService.sendWarningEmail("test@example.com", "Test Subject", "Test Body");

        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    public void testEmailServiceRetry_ExhaustedRetries() {
        doThrow(new MailSendException("Permanent SMTP Failure"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThrows(RuntimeException.class, () -> {
            emailService.sendWarningEmail("test@example.com", "Test Subject", "Test Body");
        });

        verify(mailSender, times(4)).send(any(MimeMessage.class));
    }

    @Test
    public void testCtipStatusSync_UpdateMultipleClosedStatus() {
        JobHistory dummyJob = new JobHistory();
        dummyJob.setJobName("TEST_JOB");
        dummyJob.setStartTime(LocalDateTime.now());
        dummyJob.setStatus("RUNNING");
        dummyJob = jobHistoryRepository.save(dummyJob);

        // Setup initial leaks in DB
        CredentialLeak leak1 = new CredentialLeak();
        leak1.setJob(dummyJob);
        leak1.setCredentialId("CRED-TEST-001");
        leak1.setUsername("user1@gmail.com");
        leak1.setPasswordEncrypted("pass123");
        leak1.setEmail("user1@gmail.com");
        leak1.setStatusId("STATUS_UUID_1");
        leak1.setSeverity("high");
        leak1.setLocalStatus("PENDING");
        leak1.setCtipStatus("open");
        leak1.setCtipCreatedAt(LocalDateTime.now());
        leak1.setCreatedAt(LocalDateTime.now());

        CredentialLeak leak2 = new CredentialLeak();
        leak2.setJob(dummyJob);
        leak2.setCredentialId("CRED-TEST-002");
        leak2.setUsername("user2@gmail.com");
        leak2.setPasswordEncrypted("pass123");
        leak2.setEmail("user2@gmail.com");
        leak2.setStatusId("STATUS_UUID_2");
        leak2.setSeverity("medium");
        leak2.setLocalStatus("PENDING");
        leak2.setCtipStatus("open");
        leak2.setCtipCreatedAt(LocalDateTime.now());
        leak2.setCreatedAt(LocalDateTime.now());

        credentialLeakRepository.saveAll(List.of(leak1, leak2));

        doNothing().when(mailSender).send(any(MimeMessage.class));

        leakProcessorService.processJob2SendEmailsAndSyncCtip();

        verify(ctipClient, atLeastOnce()).updateCtipStatus(anyList(), eq("close"));

        List<CredentialLeak> updatedLeaks = credentialLeakRepository.findAll();
        for (CredentialLeak leak : updatedLeaks) {
            assertEquals("PROCESSED", leak.getLocalStatus());
            assertEquals("close", leak.getCtipStatus());
        }
    }
}
