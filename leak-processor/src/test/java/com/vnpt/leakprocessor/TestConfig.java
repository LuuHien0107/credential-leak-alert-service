package com.vnpt.leakprocessor;

import com.vnpt.leakprocessor.client.CtipClient;
import com.vnpt.leakprocessor.client.SmartCaClient;
import com.vnpt.leakprocessor.client.impl.CtipClientImpl;
import com.vnpt.leakprocessor.client.impl.SmartCaClientImpl;
import com.vnpt.leakprocessor.dto.CtipLeakResponse;
import com.vnpt.leakprocessor.dto.SmartCaUserInfoResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

/**
 * Cấu hình riêng cho môi trường Test: Tự động lắng nghe cổng ngẫu nhiên (random port) của Tomcat từ WebServerInitializedEvent.
 */
@TestConfiguration
public class TestConfig implements ApplicationListener<WebServerInitializedEvent> {

    private volatile int port = 8080;

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        this.port = event.getWebServer().getPort();
    }

    @Bean
    @Primary
    public CtipClient testCtipClient(
            @Qualifier("ctipRestTemplate") RestTemplate restTemplate,
            @Value("${ctip.api.key}") String apiKey) {

        return new CtipClient() {
            private String getBaseUrl() {
                return "http://localhost:" + port;
            }

            @Override
            public CtipLeakResponse fetchLeaks(String credentialSearch, LocalDate createdAtGte, LocalDate createdAtLte, int page, int size) {
                String enrichmentUrl = getBaseUrl() + "/api/v1/account-leak/credentials/enrichment";
                String statusesUrl = getBaseUrl() + "/api/v1/account-leak/statuses";
                return new CtipClientImpl(restTemplate, apiKey, enrichmentUrl, statusesUrl)
                        .fetchLeaks(credentialSearch, createdAtGte, createdAtLte, page, size);
            }

            @Override
            public void updateCtipStatus(List<String> ids, String status) {
                String enrichmentUrl = getBaseUrl() + "/api/v1/account-leak/credentials/enrichment";
                String statusesUrl = getBaseUrl() + "/api/v1/account-leak/statuses";
                new CtipClientImpl(restTemplate, apiKey, enrichmentUrl, statusesUrl)
                        .updateCtipStatus(ids, status);
            }
        };
    }

    @Bean
    @Primary
    public SmartCaClient testSmartCaClient(
            @Qualifier("smartCaRestTemplate") RestTemplate restTemplate,
            @Value("${smartca.api.client-id}") String clientId,
            @Value("${smartca.api.client-secret}") String clientSecret) {

        return new SmartCaClient() {
            private String getBaseUrl() {
                return "http://localhost:" + port;
            }

            @Override
            public String loginAndGetToken(String username, String password) {
                String tokenUrl = getBaseUrl() + "/auth/token";
                String userInfoUrl = getBaseUrl() + "/identityapi/userinfo/info";
                return new SmartCaClientImpl(restTemplate, tokenUrl, userInfoUrl, clientId, clientSecret)
                        .loginAndGetToken(username, password);
            }

            @Override
            public SmartCaUserInfoResponse getUserInfo(String accessToken) {
                String tokenUrl = getBaseUrl() + "/auth/token";
                String userInfoUrl = getBaseUrl() + "/identityapi/userinfo/info";
                return new SmartCaClientImpl(restTemplate, tokenUrl, userInfoUrl, clientId, clientSecret)
                        .getUserInfo(accessToken);
            }
        };
    }
}
