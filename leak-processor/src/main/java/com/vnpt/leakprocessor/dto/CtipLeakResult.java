package com.vnpt.leakprocessor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Đối tượng DTO mô tả chi tiết thông tin một tài khoản bị rò rỉ trả về từ API CTIP.
 */
public class CtipLeakResult {

    @JsonProperty("credential_id")
    private String credentialId;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("group_id")
    private String groupId;

    @JsonProperty("group_name")
    private String groupName;

    @JsonProperty("first_compromise_time")
    private String firstCompromiseTime;

    @JsonProperty("url")
    private String url;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("status_id")
    private String statusId;

    @JsonProperty("status")
    private String status;

    // Hàm khởi tạo mặc định
    public CtipLeakResult() {}

    // Các phương thức Getter và Setter
    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getFirstCompromiseTime() {
        return firstCompromiseTime;
    }

    public void setFirstCompromiseTime(String firstCompromiseTime) {
        this.firstCompromiseTime = firstCompromiseTime;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatusId() {
        return statusId;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
