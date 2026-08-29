package com.vnpt.leakprocessor.repository;

import com.vnpt.leakprocessor.model.CredentialLeak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy xuất cơ sở dữ liệu cho thực thể CredentialLeak.
 */
@Repository
public interface CredentialLeakRepository extends JpaRepository<CredentialLeak, Long> {

    // Kiểm tra xem mã rò rỉ credentialId đã tồn tại hay chưa
    boolean existsByCredentialId(String credentialId);

    // Tìm kiếm sự cố lộ lọt theo credentialId
    Optional<CredentialLeak> findByCredentialId(String credentialId);

    // Tìm danh sách các sự cố lộ lọt theo trạng thái cục bộ
    List<CredentialLeak> findByLocalStatus(String localStatus);

    // Tìm danh sách các sự cố lộ lọt khớp với tập hợp nhiều trạng thái
    List<CredentialLeak> findByLocalStatusIn(List<String> statuses);

    // Đếm tổng số lượng sự cố theo trạng thái cục bộ
    long countByLocalStatus(String localStatus);

    // Đếm tổng số lượng sự cố theo cấp độ nghiêm trọng (Severity)
    long countBySeverity(String severity);

    // Tìm danh sách các sự cố lộ lọt thuộc một lượt chạy Job cụ thể
    List<CredentialLeak> findByJobId(Long jobId);
}
