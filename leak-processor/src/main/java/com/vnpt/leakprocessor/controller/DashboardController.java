package com.vnpt.leakprocessor.controller;

import com.vnpt.leakprocessor.model.CredentialLeak;
import com.vnpt.leakprocessor.model.JobHistory;
import com.vnpt.leakprocessor.model.ProcessingLog;
import com.vnpt.leakprocessor.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Controller xử lý giao diện người dùng Thymeleaf cho màn hình Dashboard, Lịch sử Job và danh sách sự cố lộ lọt.
 * Đã được chuẩn hóa architecture: Ủy quyền toàn bộ việc tra cứu dữ liệu xuống tầng DashboardService.
 */
@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Điều hướng trang chủ mặc định sang trang /dashboard.
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    /**
     * Hiển thị trang tổng quan Dashboard thống kê các chỉ số lộ lọt và đồ thị.
     */
    @GetMapping("/dashboard")
    public String getDashboard(Model model) {
        Map<String, Object> stats = dashboardService.getDashboardStatsMap();

        model.addAttribute("totalLeaks", stats.get("totalLeaks"));
        model.addAttribute("totalPending", stats.get("totalPending"));
        model.addAttribute("totalEmailed", stats.get("totalEmailed"));
        model.addAttribute("totalProcessed", stats.get("totalProcessed"));
        model.addAttribute("totalFailed", stats.get("totalFailed"));

        model.addAttribute("severityLow", stats.get("severityLow"));
        model.addAttribute("severityMedium", stats.get("severityMedium"));
        model.addAttribute("severityHigh", stats.get("severityHigh"));
        model.addAttribute("severityCritical", stats.get("severityCritical"));

        model.addAttribute("latestLeaks", stats.get("rawLatestLeaks"));
        model.addAttribute("activeTab", "dashboard");

        return "dashboard";
    }

    /**
     * Hiển thị toàn bộ danh sách tất cả các sự cố lộ lọt đã quét được từ trước tới nay.
     */
    @GetMapping("/dashboard/leaks")
    public String getLeaks(Model model) {
        List<CredentialLeak> leaks = dashboardService.getAllLeaks();
        model.addAttribute("leaks", leaks);
        model.addAttribute("activeTab", "dashboard");
        return "dashboard/leaks";
    }

    /**
     * Hiển thị trang lịch sử chạy tiến trình (Job History).
     */
    @GetMapping("/dashboard/jobs")
    public String getJobs(Model model) {
        List<JobHistory> jobs = dashboardService.getAllJobs();
        model.addAttribute("jobs", jobs);
        model.addAttribute("activeTab", "jobs");
        return "dashboard/jobs";
    }

    /**
     * Hiển thị chi tiết thông tin và kết quả xử lý của một lượt chạy Job cụ thể dựa theo ID.
     */
    @GetMapping("/dashboard/jobs/{id}")
    public String getJobDetails(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        JobHistory job = dashboardService.getJobById(id);
        if (job == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông tin lượt chạy Job yêu cầu.");
            return "redirect:/dashboard/jobs";
        }

        List<CredentialLeak> leaks = dashboardService.getLeaksByJobId(id);
        List<ProcessingLog> logs = dashboardService.getLogsByJobId(id);

        model.addAttribute("job", job);
        model.addAttribute("leaks", leaks);
        model.addAttribute("logs", logs);
        model.addAttribute("activeTab", "jobs");
        return "dashboard/job-details";
    }
}
