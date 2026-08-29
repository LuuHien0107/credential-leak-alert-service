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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
class LeakProcessorTests {

    @Autowired
    private LeakProcessorService leakProcessorService;

    @Autowired
    private CtipClient ctipClient;

    @Autowired
    private CredentialLeakRepository credentialLeakRepository;

    @Autowired
    private JobHistoryRepository jobHistoryRepository;

    @Autowired
    private ProcessingLogRepository processingLogRepository;

    @BeforeEach
    void setUp() {
        processingLogRepository.deleteAllInBatch();
        credentialLeakRepository.deleteAllInBatch();
        jobHistoryRepository.deleteAllInBatch();
    }

    @Test
    void testProcessLeaks_NewLeaksStoredAsPending() {
        leakProcessorService.processJob1FetchAndVerify();

        Optional<CredentialLeak> leakOpt = credentialLeakRepository.findByCredentialId("019adf91-e7e5-7061-bf20-3971f1a44ab7");
        assertTrue(leakOpt.isPresent(), "The new leak should be saved in the database");
        
        CredentialLeak leak = leakOpt.get();
        assertEquals("PENDING", leak.getLocalStatus(), "New leak status should be PENDING");
        assertNotNull(leak.getEmail(), "Leak email should be set");

        List<JobHistory> jobs = jobHistoryRepository.findAll();
        assertFalse(jobs.isEmpty(), "JobHistory should record an execution entry");
        assertEquals("SUCCESS", jobs.get(0).getStatus());

        List<ProcessingLog> logs = processingLogRepository.findAll();
        assertFalse(logs.isEmpty(), "ProcessingLogs should record step details");
    }

    @Test
    void testProcessLeaks_DuplicateLeaksIgnoredAndNotResetStatus() {
        leakProcessorService.processJob1FetchAndVerify();
        
        Optional<CredentialLeak> leakOpt = credentialLeakRepository.findByCredentialId("019adf91-e7e5-7061-bf20-3971f1a44ab7");
        assertTrue(leakOpt.isPresent());
        CredentialLeak leak = leakOpt.get();
        leak.setLocalStatus("PROCESSED");
        credentialLeakRepository.save(leak);

        leakProcessorService.processJob1FetchAndVerify();

        CredentialLeak reFetchedLeak = credentialLeakRepository.findByCredentialId("019adf91-e7e5-7061-bf20-3971f1a44ab7").get();
        assertEquals("PROCESSED", reFetchedLeak.getLocalStatus(), "Status of existing processed leak must NOT be overwritten back to PENDING");
    }
}
