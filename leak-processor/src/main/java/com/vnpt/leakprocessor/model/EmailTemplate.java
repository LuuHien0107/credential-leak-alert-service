package com.vnpt.leakprocessor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Thực thể lưu trữ cấu trúc mẫu Email HTML cảnh báo bảo mật trong cơ sở dữ liệu.
 */
@Entity
@Table(name = "email_templates", indexes = {
    @Index(name = "idx_templates_name", columnList = "template_name", unique = true)
})
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "template_name", length = 100, nullable = false, unique = true)
    private String templateName;

    @Column(name = "subject", length = 255, nullable = false)
    private String subject;

    @Column(name = "body_html", nullable = false, columnDefinition = "TEXT")
    private String bodyHtml;

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

    // Hàm khởi tạo không tham số mặc định
    public EmailTemplate() {}

    // Hàm khởi tạo đầy đủ tham số
    public EmailTemplate(String templateName, String subject, String bodyHtml) {
        this.templateName = templateName;
        this.subject = subject;
        this.bodyHtml = bodyHtml;
    }

    // Các hàm Getter và Setter
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
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
