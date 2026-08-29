package com.vnpt.leakprocessor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LeakProcessorApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context loads successfully,
        // which triggers Hibernate to sync and auto-create database tables in MySQL.
    }
}
