package com.vnpt.leakprocessor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Lớp cấu hình Spring Bean cho HTTP RestTemplate client chuyên biệt.
 * Cấu hình timeout độc lập cho cổng VNPT CTIP API và cổng VNPT SmartCA Gateway.
 */
@Configuration
public class RestClientConfig {

    /**
     * RestTemplate Bean dành riêng cho kết nối VNPT CTIP API.
     */
    @Bean(name = "ctipRestTemplate")
    public RestTemplate ctipRestTemplate(
            @Value("${ctip.api.connect-timeout-ms:5000}") int connectTimeout,
            @Value("${ctip.api.read-timeout-ms:10000}") int readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }

    /**
     * RestTemplate Bean dành riêng cho kết nối VNPT SmartCA Gateway.
     */
    @Bean(name = "smartCaRestTemplate")
    public RestTemplate smartCaRestTemplate(
            @Value("${smartca.api.connect-timeout-ms:5000}") int connectTimeout,
            @Value("${smartca.api.read-timeout-ms:10000}") int readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }

    /**
     * RestTemplate Bean dành riêng cho kết nối Telegram Bot API.
     */
    @Bean(name = "telegramRestTemplate")
    public RestTemplate telegramRestTemplate(
            @Value("${telegram.bot.connect-timeout-ms:5000}") int connectTimeout,
            @Value("${telegram.bot.read-timeout-ms:5000}") int readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
}
