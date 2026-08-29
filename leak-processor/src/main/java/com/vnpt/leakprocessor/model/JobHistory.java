package com.vnpt.leakprocessor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Thực thể ghi nhận lịch sử và kết quả thực thi của từng Job quét (Tự động hoặc Thủ công).
 */
@Entity
@Table(name = "job_history")
public class JobHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", length = 100, nullable = false)
    private String jobName;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "status", length = 20, nullable = false)
    private String status; // Giá trị: RUNNING, SUCCESS, FAILED

    @Column(name = "total_fetched")
    private Integer totalFetched = 0;

    @Column(name = "total_mapped")
    private Integer totalMapped = 0;

    @Column(name = "total_sent_email")
    private Integer totalSentEmail = 0;

    @Column(name = "total_updated_ctip")
    private Integer totalUpdatedCtip = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Hàm khởi tạo mặc định
    public JobHistory() {}

    // Hàm khởi tạo nhanh có tham số
    public JobHistory(String jobName, LocalDateTime startTime, String status) {
        this.jobName = jobName;
        this.startTime = startTime;
        this.status = status;
        this.totalFetched = 0;
        this.totalMapped = 0;
        this.totalSentEmail = 0;
        this.totalUpdatedCtip = 0;
    }

    // Các hàm Getter và Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalFetched() {
        return totalFetched;
    }

    public void setTotalFetched(Integer totalFetched) {
        this.totalFetched = totalFetched;
    }

    public Integer getTotalMapped() {
        return totalMapped;
    }

    public void setTotalMapped(Integer totalMapped) {
        this.totalMapped = totalMapped;
    }

    public Integer getTotalSentEmail() {
        return totalSentEmail;
    }

    public void setTotalSentEmail(Integer totalSentEmail) {
        this.totalSentEmail = totalSentEmail;
    }

    public Integer getTotalUpdatedCtip() {
        return totalUpdatedCtip;
    }

    public void setTotalUpdatedCtip(Integer totalUpdatedCtip) {
        this.totalUpdatedCtip = totalUpdatedCtip;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
