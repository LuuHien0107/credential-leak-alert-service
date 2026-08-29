package com.vnpt.leakprocessor.service;

import com.vnpt.leakprocessor.model.CredentialLeak;
import com.vnpt.leakprocessor.model.JobHistory;
import com.vnpt.leakprocessor.model.ProcessingLog;

import java.util.List;
import java.util.Map;

/**
 * Interface dịch vụ xử lý nghiệp vụ thống kê Dashboard và tra cứu dữ liệu Lịch sử Job & Sự cố rò rỉ.
 */
public interface DashboardService {

    /**
     * Lấy dữ liệu thống kê tổng hợp thời gian thực (Counts, Severity, 5 latest leaks) phục vụ Dashboard UI và REST API.
     */
    Map<String, Object> getDashboardStatsMap();

    /**
     * Lấy danh sách toàn bộ các sự cố rò rỉ tài khoản đã quét được (sắp xếp giảm dần theo ID).
     */
    List<CredentialLeak> getAllLeaks();

    /**
     * Lấy danh sách toàn bộ lịch sử các lượt chạy Job (sắp xếp giảm dần theo ID).
     */
    List<JobHistory> getAllJobs();

    /**
     * Tìm thông tin một lượt chạy Job cụ thể dựa theo ID.
     */
    JobHistory getJobById(Long id);

    /**
     * Khởi tạo và lưu bản ghi JobHistory mới với trạng thái RUNNING phục vụ chạy Async Job thủ công.
     */
    JobHistory createAndSaveJobHistory(String jobName);

    /**
     * Tìm danh sách tất cả các sự cố rò rỉ tài khoản được xử lý trong một lượt chạy Job cụ thể.
     */
    List<CredentialLeak> getLeaksByJobId(Long jobId);

    /**
     * Tìm danh sách nhật ký xử lý chi tiết (Processing Logs) của một lượt chạy Job cụ thể.
     */
    List<ProcessingLog> getLogsByJobId(Long jobId);
}
