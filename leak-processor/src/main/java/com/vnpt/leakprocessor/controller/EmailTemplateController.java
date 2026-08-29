package com.vnpt.leakprocessor.controller;

import com.vnpt.leakprocessor.model.EmailTemplate;
import com.vnpt.leakprocessor.repository.EmailTemplateRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controller xử lý giao diện người dùng Thymeleaf liên quan đến quản lý các mẫu
 * email cảnh báo (Email Templates CRUD).
 */
@Controller
public class EmailTemplateController {

    private final EmailTemplateRepository emailTemplateRepository;

    public EmailTemplateController(EmailTemplateRepository emailTemplateRepository) {
        this.emailTemplateRepository = emailTemplateRepository;
    }

    /**
     * Hiển thị danh sách tất cả các mẫu email cảnh báo rò rỉ đang lưu trữ trong cơ
     * sở dữ liệu.
     */
    @GetMapping("/templates")
    public String listTemplates(Model model) {
        java.util.List<EmailTemplate> templates = emailTemplateRepository.findAll(Sort.by("id").descending());
        model.addAttribute("templates", templates);
        model.addAttribute("activeTab", "templates");
        return "templates/list";
    }

    /**
     * Hiển thị form chỉnh sửa nội dung tiêu đề và mã HTML của một mẫu email cụ thể
     * dựa theo ID.
     */
    @GetMapping("/templates/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        EmailTemplate template = emailTemplateRepository.findById(id).orElse(null);
        if (template == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy mẫu email yêu cầu.");
            return "redirect:/templates";
        }
        model.addAttribute("template", template);
        model.addAttribute("activeTab", "templates");
        return "templates/form";
    }

    /**
     * Lưu lại nội dung mẫu email cảnh báo sau khi đã chỉnh sửa cấu trúc HTML hoặc
     * tiêu đề.
     * Đảm bảo giữ nguyên các trường tên mẫu (templateName) và thời gian tạo ban
     * đầu.
     */
    @PostMapping("/templates/save")
    public String saveTemplate(@ModelAttribute("template") EmailTemplate template,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            EmailTemplate existing = emailTemplateRepository.findById(template.getId()).orElse(null);
            if (existing != null) {
                // Giữ nguyên tên mẫu và thời gian khởi tạo trong cơ sở dữ liệu
                template.setTemplateName(existing.getTemplateName());
                template.setCreatedAt(existing.getCreatedAt());
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Mẫu email cảnh báo không tồn tại trong hệ thống.");
                return "redirect:/templates";
            }
            template.setUpdatedAt(java.time.LocalDateTime.now());
            emailTemplateRepository.save(template);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã lưu cập nhật mẫu email '" + template.getTemplateName() + "' thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Lỗi phát sinh khi lưu cập nhật mẫu email: " + e.getMessage());
        }
        return "redirect:/templates";
    }
}
