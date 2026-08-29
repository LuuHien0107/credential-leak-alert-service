package com.vnpt.leakprocessor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Thực thể lưu trữ chi tiết thông tin tài khoản bị lộ lọt (Credential Leak).
 * Lưu trữ thông tin tài khoản, email, số điện thoại thu thập từ SmartCA và lưu thuộc job nào (JobHistory).
 */
@Entity
@Table(name = "credential_leaks", indexes = {
    @Index(name = "idx_leaks_credential_id", columnList = "credential_id", unique = true),
    @Index(name = "idx_leaks_status_id", columnList = "status_id"),
    @Index(name = "idx_leaks_local_status", columnList = "local_status"),
    @Index(name = "idx_leaks_job_id", columnList = "job_id")
})
public class CredentialLeak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "credential_id", length = 100, nullable = false, unique = true)
    private String credentialId;

    @Column(name = "status_id", length = 100, nullable = false)
    private String statusId;

    @Column(name = "username", length = 100, nullable = false)
    private String username;

    @Column(name = "password_encrypted", length = 512, nullable = false)
    private String passwordEncrypted;

    @Column(name = "severity", length = 20, nullable = false)
    private String severity;

    @Column(name = "compromise_time")
    private LocalDateTime compromiseTime;

    @Column(name = "leak_url", length = 2048)
    private String leakUrl;

    @Column(name = "ctip_created_at", nullable = false)
    private LocalDateTime ctipCreatedAt;

    @Column(name = "ctip_status", length = 20, nullable = false)
    private String ctipStatus;

    @Column(name = "local_status", length = 30, nullable = false)
    private String localStatus;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, foreignKey = @ForeignKey(name = "fk_leaks_job_history"))
    private JobHistory job;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Hàm khởi tạo mặc định
    public CredentialLeak() {}

    // Các hàm Getter và Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getStatusId() {
        return statusId;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordEncrypted() {
        return passwordEncrypted;
    }

    public void setPasswordEncrypted(String passwordEncrypted) {
        this.passwordEncrypted = passwordEncrypted;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public LocalDateTime getCompromiseTime() {
        return compromiseTime;
    }

    public void setCompromiseTime(LocalDateTime compromiseTime) {
        this.compromiseTime = compromiseTime;
    }

    public String getLeakUrl() {
        return leakUrl;
    }

    public void setLeakUrl(String leakUrl) {
        this.leakUrl = leakUrl;
    }

    public LocalDateTime getCtipCreatedAt() {
        return ctipCreatedAt;
    }

    public void setCtipCreatedAt(LocalDateTime ctipCreatedAt) {
        this.ctipCreatedAt = ctipCreatedAt;
    }

    public String getCtipStatus() {
        return ctipStatus;
    }

    public void setCtipStatus(String ctipStatus) {
        this.ctipStatus = ctipStatus;
    }

    public String getLocalStatus() {
        return localStatus;
    }

    public void setLocalStatus(String localStatus) {
        this.localStatus = localStatus;
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

    public JobHistory getJob() {
        return job;
    }

    public void setJob(JobHistory job) {
        this.job = job;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
