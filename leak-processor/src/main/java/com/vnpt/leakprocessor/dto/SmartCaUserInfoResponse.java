package com.vnpt.leakprocessor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO đại diện cho response trả về từ API lấy thông tin người dùng của VNPT SmartCA (/identityapi/userinfo/info).
 * Ánh xạ duy nhất 2 trường email và phone trong phần nội dung (content), các trường dư thừa khác sẽ tự động bỏ qua.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SmartCaUserInfoResponse {

    private Integer code;
    private String codeDesc;
    private String message;
    private UserInfoContent content;

    public SmartCaUserInfoResponse() {}

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getCodeDesc() {
        return codeDesc;
    }

    public void setCodeDesc(String codeDesc) {
        this.codeDesc = codeDesc;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UserInfoContent getContent() {
        return content;
    }

    public void setContent(UserInfoContent content) {
        this.content = content;
    }

    /**
     * Lớp nội dung thông tin người dùng chỉ lưu 2 thuộc tính cần thiết: email và phone.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserInfoContent {

        private String email;
        private String phone;

        public UserInfoContent() {}

        public UserInfoContent(String email, String phone) {
            this.email = email;
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }
}
