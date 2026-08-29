package com.vnpt.leakprocessor.dto;

import java.util.List;

/**
 * Đối tượng DTO chứa dữ liệu phản hồi danh sách tài khoản rò rỉ phân trang từ API CTIP.
 */
public class CtipLeakResponse {

    private List<CtipLeakResult> results;
    private Integer total;
    private Integer page;
    private Integer size;
    private Integer pages;

    // Hàm khởi tạo không đối số mặc định
    public CtipLeakResponse() {}

    // Hàm khởi tạo đầy đủ tham số
    public CtipLeakResponse(List<CtipLeakResult> results, Integer total, Integer page, Integer size, Integer pages) {
        this.results = results;
        this.total = total;
        this.page = page;
        this.size = size;
        this.pages = pages;
    }

    // Các phương thức Getter và Setter truy xuất thuộc tính
    public List<CtipLeakResult> getResults() {
        return results;
    }

    public void setResults(List<CtipLeakResult> results) {
        this.results = results;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }
}
