package com.vnpt.leakprocessor.repository;

import com.vnpt.leakprocessor.model.JobHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository truy xuất cơ sở dữ liệu cho thực thể JobHistory.
 */
@Repository
public interface JobHistoryRepository extends JpaRepository<JobHistory, Long> {
}
