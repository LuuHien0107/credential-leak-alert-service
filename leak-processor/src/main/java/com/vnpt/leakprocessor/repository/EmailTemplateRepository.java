package com.vnpt.leakprocessor.repository;

import com.vnpt.leakprocessor.model.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repository truy xuất cơ sở dữ liệu cho thực thể EmailTemplate.
 */
@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Integer> {
    
    // Tìm kiếm mẫu thư cảnh báo dựa trên tên mẫu (templateName) duy nhất
    Optional<EmailTemplate> findByTemplateName(String templateName);
}
