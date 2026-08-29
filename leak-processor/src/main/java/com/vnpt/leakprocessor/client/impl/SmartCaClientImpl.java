package com.vnpt.leakprocessor.client.impl;

import com.vnpt.leakprocessor.client.SmartCaClient;
import com.vnpt.leakprocessor.dto.SmartCaTokenResponse;
import com.vnpt.leakprocessor.dto.SmartCaUserInfoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Service triển khai tích hợp cổng VNPT SmartCA Gateway (gwsca.vnpt.vn).
 * Gọi API xác thực đăng nhập lấy Bearer token và truy vấn thông tin email & phone người dùng.
 */
@Service
public class SmartCaClientImpl implements SmartCaClient {

    private static final Logger logger = LoggerFactory.getLogger(SmartCaClientImpl.class);

    private final RestTemplate restTemplate;
    private final String tokenUrl;
    private final String userInfoUrl;
    private final String clientId;
    private final String clientSecret;

    public SmartCaClientImpl(
            @Qualifier("smartCaRestTemplate") RestTemplate restTemplate,
            @Value("${smartca.api.token-url}") String tokenUrl,
            @Value("${smartca.api.userinfo-url}") String userInfoUrl,
            @Value("${smartca.api.client-id}") String clientId,
            @Value("${smartca.api.client-secret}") String clientSecret) {
        this.restTemplate = restTemplate;
        this.tokenUrl = tokenUrl;
        this.userInfoUrl = userInfoUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * Gọi API SmartCA Auth `/auth/token` với thông tin đăng nhập của người dùng.
     * Content-Type: application/x-www-form-urlencoded
     */
    @Override
    public String loginAndGetToken(String username, String password) {
        logger.info("SmartCaClient: Dang goi API dang nhap SmartCA ({}) cho username='{}'", tokenUrl, username);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "password");
        body.add("username", username);
        body.add("password", password);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<SmartCaTokenResponse> response = restTemplate.postForEntity(tokenUrl, requestEntity, SmartCaTokenResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().getAccessToken() != null) {
                logger.info("SmartCaClient: Dang nhap SmartCA thanh cong cho username='{}'", username);
                return response.getBody().getAccessToken();
            }
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            logger.warn("SmartCaClient: Dang nhap SmartCA thất bại cho username='{}' (Status: {})", username, e.getStatusCode());
        } catch (Exception e) {
            logger.error("SmartCaClient: Loi khi goi SmartCA Token API cho username='{}': {}", username, e.getMessage(), e);
        }

        return null;
    }

    /**
     * Gọi API SmartCA Identity `/identityapi/userinfo/info` để lấy email và phone.
     * Header: Authorization: Bearer <accessToken>
     */
    @Override
    public SmartCaUserInfoResponse getUserInfo(String accessToken) {
        logger.info("SmartCaClient: Dang goi API UserInfo SmartCA ({})", userInfoUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<SmartCaUserInfoResponse> response = restTemplate.postForEntity(userInfoUrl, requestEntity, SmartCaUserInfoResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("SmartCaClient: Lay thong tin nguoi dung SmartCA thanh cong");
                return response.getBody();
            }
        } catch (Exception e) {
            logger.error("SmartCaClient: Loi khi goi SmartCA UserInfo API: {}", e.getMessage(), e);
        }

        return null;
    }
}
