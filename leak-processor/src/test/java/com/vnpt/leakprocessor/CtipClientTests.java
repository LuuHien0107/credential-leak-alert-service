package com.vnpt.leakprocessor;

import com.vnpt.leakprocessor.client.CtipClient;
import com.vnpt.leakprocessor.dto.CtipLeakResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestConfig.class)
class CtipClientTests {

    @Autowired
    private CtipClient ctipClient;

    @Test
    void testFetchAllLeaksNoFilters() {
        CtipLeakResponse response = ctipClient.fetchLeaks("", null, null, 1, 100);
        assertNotNull(response);
        assertEquals(23, response.getTotal());
        assertEquals(23, response.getResults().size());
        assertEquals(1, response.getPage());
        assertEquals(1, response.getPages());
    }

    @Test
    void testFetchLeaksWithSearchFilter() {
        CtipLeakResponse response = ctipClient.fetchLeaks("ausca.vnpt.vn", null, null, 1, 100);
        assertNotNull(response);
        
        response.getResults().forEach(result -> 
            assertTrue(result.getUrl().toLowerCase().contains("ausca.vnpt.vn"))
        );
        
        assertEquals(10, response.getTotal());
    }

    @Test
    void testFetchLeaksWithPagination() {
        CtipLeakResponse responsePage1 = ctipClient.fetchLeaks("", null, null, 1, 5);
        assertNotNull(responsePage1);
        assertEquals(23, responsePage1.getTotal());
        assertEquals(5, responsePage1.getResults().size());
        assertEquals(5, responsePage1.getPages());
    }
}
