package com.vnpt.leakprocessor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Thực thể lưu vết log chi tiết tiến trình xử lý từng tài khoản lộ lọt trong hệ thống.
 * Hỗ trợ ghi lại stack trace lỗi khi xảy ra lỗi phát sinh (Gửi mail lỗi, Đồng bộ lỗi...).
 */
@Entity
@Table(name = "processing_logs", indexes = {
    @Index(name = "idx_logs_job_id", columnList = "job_id"),
    @Index(name = "idx_logs_leak_id", columnList = "leak_id")
})
public class ProcessingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leak_id", foreignKey = @ForeignKey(name = "fk_logs_leak"))
    private CredentialLeak leak;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, foreignKey = @ForeignKey(name = "fk_logs_job_history"))
    private JobHistory job;

    @Column(name = "step", length = 50, nullable = false)
    private String step; // Giá trị: FETCH, MAPPING, EMAIL, CLOSE, START, END

    @Column(name = "status", length = 20, nullable = false)
    private String status; // Giá trị: SUCCESS, FAILED

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Hàm khởi tạo mặc định
    public ProcessingLog() {}

    // Hàm khởi tạo đầy đủ tham số
    public ProcessingLog(JobHistory job, CredentialLeak leak, String step, String status, String message, String stackTrace) {
        this.job = job;
        this.leak = leak;
        this.step = step;
        this.status = status;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    // Các hàm Getter và Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CredentialLeak getLeak() {
        return leak;
    }

    public void setLeak(CredentialLeak leak) {
        this.leak = leak;
    }

    public JobHistory getJob() {
        return job;
    }

    public void setJob(JobHistory job) {
        this.job = job;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
