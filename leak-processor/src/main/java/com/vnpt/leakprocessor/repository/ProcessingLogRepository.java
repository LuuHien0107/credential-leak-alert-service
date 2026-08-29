package com.vnpt.leakprocessor.repository;

import com.vnpt.leakprocessor.model.ProcessingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository truy xuất cơ sở dữ liệu cho thực thể ProcessingLog.
 */
@Repository
public interface ProcessingLogRepository extends JpaRepository<ProcessingLog, Long> {
    
    // Tìm danh sách các log chi tiết thuộc một lượt chạy Job cụ thể
    List<ProcessingLog> findByJobId(Long jobId);
    
    // Tìm danh sách các log chi tiết thuộc một sự cố lộ lọt cụ thể
    List<ProcessingLog> findByLeakId(Long leakId);
}
