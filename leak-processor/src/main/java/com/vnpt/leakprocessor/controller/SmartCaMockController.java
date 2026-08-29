package com.vnpt.leakprocessor.controller;

import com.vnpt.leakprocessor.dto.SmartCaTokenResponse;
import com.vnpt.leakprocessor.dto.SmartCaUserInfoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * RestController giả lập (Mock) cho các API của cổng VNPT SmartCA Gateway
 * (gwsca.vnpt.vn).
 * Phục vụ chạy kiểm thử local offline mà không cần kết nối internet tới máy chủ
 * SmartCA thật.
 */
@RestController
public class SmartCaMockController {

    private static final Logger logger = LoggerFactory.getLogger(SmartCaMockController.class);

    private static final String MOCK_CLIENT_ID = "4185-637127995547330633.apps.signserviceapi.com";
    private static final String MOCK_CLIENT_SECRET = "NGNhMzdmOGE-OGM2Mi00MTg0";

    /**
     * Endpoint giả lập API lấy Token đăng nhập của SmartCA.
     * URL: POST /auth/token (Content-Type: application/x-www-form-urlencoded)
     */
    @PostMapping(value = "/auth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> mockToken(
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam(value = "grant_type", required = false) String grantType,
            @RequestParam String username,
            @RequestParam String password) {

        logger.info("Mock SmartCA Server: Nhan yeu cau POST /auth/token cho username='{}'", username);

        // Kiểm tra Client ID & Client Secret
        if (clientId == null || !clientId.equals(MOCK_CLIENT_ID) || clientSecret == null
                || !clientSecret.equals(MOCK_CLIENT_SECRET)) {
            logger.warn("Mock SmartCA Server: Tu choi do Client ID hoac Client Secret khong hop le.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "invalid_client",
                            "error_description", "Invalid client credentials"));
        }

        // Giả lập từ chối nếu sai thông tin tài khoản hoặc mật khẩu đánh dấu là
        // "INVALID_PASS" / "wrong_pass"
        if (password != null
                && (password.equalsIgnoreCase("INVALID_PASS") || password.equalsIgnoreCase("wrong_pass"))) {
            logger.warn("Mock SmartCA Server: Tu choi dang nhap cho username='{}' do sai mat khau.", username);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "invalid_grant",
                            "error_description", "Bad credentials / Username or password incorrect"));
        }

        // Đăng nhập thành công -> Sinh token mã hóa chứa username để phục vụ trả email
        // & phone động
        String mockToken = "mock_token_" + username + "___" + UUID.randomUUID().toString();
        SmartCaTokenResponse tokenResponse = new SmartCaTokenResponse();
        tokenResponse.setAccessToken(mockToken);
        tokenResponse.setTokenType("Bearer");
        tokenResponse.setExpiresIn(3600);

        logger.info("Mock SmartCA Server: Cap token thanh cong cho username='{}'", username);
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * Endpoint giả lập API lấy thông tin người dùng của SmartCA.
     * URL: POST /identityapi/userinfo/info (Header: Authorization: Bearer
     * <accessToken>)
     */
    @PostMapping("/identityapi/userinfo/info")
    public ResponseEntity<?> mockUserInfo(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        logger.info("Mock SmartCA Server: Nhan yeu cau POST /identityapi/userinfo/info");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Mock SmartCA Server: Tu choi truy van do thieu hoac sai dinh dang Header Authorization.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("code", 401, "message", "Unauthorized"));
        }

        String rawToken = authHeader.substring(7);
        String username = "luuhien1267";
        if (rawToken.startsWith("mock_token_") && rawToken.contains("___")) {
            username = rawToken.substring("mock_token_".length(), rawToken.indexOf("___"));
        }

        // Tạo dữ liệu giả lập linh hoạt theo tên username
        String email;
        String phone;
        if ("066095002466".equalsIgnoreCase(username)) {
            email = "luuminhhien68@gmail.com";
            phone = "0987654321";
        } else if (username.contains("@")) {
            email = username;
            phone = "09" + String.format("%08d", Math.abs(username.hashCode() % 100000000));
        } else if (username.matches("\\d+")) {
            email = username + "@invalid-test.local";
            phone = username.length() >= 10 ? username.substring(0, 10)
                    : ("09" + String.format("%08d", Math.abs(username.hashCode() % 100000000)));
        } else {
            email = username + "@invalid-test.local";
            phone = "09" + String.format("%08d", Math.abs(username.hashCode() % 100000000));
        }

        SmartCaUserInfoResponse userInfoResponse = new SmartCaUserInfoResponse();
        userInfoResponse.setCode(0);
        userInfoResponse.setCodeDesc("SUCCESS");
        userInfoResponse.setMessage("");

        SmartCaUserInfoResponse.UserInfoContent content = new SmartCaUserInfoResponse.UserInfoContent();
        content.setEmail(email);
        content.setPhone(phone);

        userInfoResponse.setContent(content);

        logger.info(
                "Mock SmartCA Server: Tra ve thong tin nguoi dung thanh cong (username='{}', email='{}', phone='{}')",
                username, content.getEmail(), content.getPhone());

        return ResponseEntity.ok(userInfoResponse);
    }
}
