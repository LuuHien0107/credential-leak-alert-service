package com.vnpt.leakprocessor.client;

import com.vnpt.leakprocessor.dto.SmartCaUserInfoResponse;

/**
 * Interface client tích hợp các API xác thực và truy vấn thông tin người dùng từ VNPT SmartCA.
 */
public interface SmartCaClient {

    /**
     * Đăng nhập lấy access_token từ VNPT SmartCA Gateway.
     *
     * @param username Tên đăng nhập tài khoản lộ lọt
     * @param password Mật khẩu tài khoản lộ lọt (chưa mã hóa)
     * @return Chuỗi access_token nếu đăng nhập thành công, hoặc null nếu tài khoản/mật khẩu sai.
     */
    String loginAndGetToken(String username, String password);

    /**
     * Lấy thông tin người dùng (Email và Phone) từ VNPT SmartCA Identity API.
     *
     * @param accessToken Mã access_token đã lấy từ bước đăng nhập
     * @return Thông tin người dùng dưới dạng DTO SmartCaUserInfoResponse
     */
    SmartCaUserInfoResponse getUserInfo(String accessToken);
}
