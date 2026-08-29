package com.vnpt.leakprocessor;

import com.vnpt.leakprocessor.client.CtipClient;
import com.vnpt.leakprocessor.model.CredentialLeak;
import com.vnpt.leakprocessor.model.JobHistory;
import com.vnpt.leakprocessor.model.ProcessingLog;
import com.vnpt.leakprocessor.repository.CredentialLeakRepository;
import com.vnpt.leakprocessor.repository.JobHistoryRepository;
import com.vnpt.leakprocessor.repository.ProcessingLogRepository;
import com.vnpt.leakprocessor.service.LeakProcessorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import jakarta.mail.internet.MimeMessage;

import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestConfig.class)
public class EndToEndPipelineTests {

    @Autowired
    private LeakProcessorService leakProcessorService;

    @Autowired
    private CredentialLeakRepository credentialLeakRepository;

    @Autowired
    private JobHistoryRepository jobHistoryRepository;

    @Autowired
    private ProcessingLogRepository processingLogRepository;

    @Autowired
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
        doNothing().when(mailSender).send(any(MimeMessage.class));
    }

    @Test
    public void testEndToEndPipeline_Success() {
        // Step 1: Execute processJob1FetchAndVerify() -> fetches from Mock CTIP, saves to DB
        leakProcessorService.processJob1FetchAndVerify();

        List<CredentialLeak> savedLeaks = credentialLeakRepository.findAll();
        assertFalse(savedLeaks.isEmpty(), "DB should contain saved leaks");

        // Verify initial state
        CredentialLeak firstLeak = savedLeaks.get(0);
        assertEquals("PENDING", firstLeak.getLocalStatus());
        assertNotNull(firstLeak.getEmail());

        // Step 2: Execute processJob2SendEmailsAndSyncCtip() -> sends warning email & closes CTIP (PROCESSED)
        leakProcessorService.processJob2SendEmailsAndSyncCtip();

        CredentialLeak updatedLeakAfterJob2 = credentialLeakRepository.findById(firstLeak.getId()).orElseThrow();
        assertEquals("PROCESSED", updatedLeakAfterJob2.getLocalStatus());

        // Verify mailSender interaction
        verify(mailSender, atLeastOnce()).send(any(MimeMessage.class));

        // Verify JobHistory recorded successful execution
        List<JobHistory> jobs = jobHistoryRepository.findAll();
        assertFalse(jobs.isEmpty());
        JobHistory lastJob = jobs.get(jobs.size() - 1);
        assertEquals("SUCCESS", lastJob.getStatus());

        // Verify ProcessingLogs generated for pipeline steps
        List<ProcessingLog> logs = processingLogRepository.findAll();
        assertFalse(logs.isEmpty());
    }
}
