package com.vnpt.leakprocessor.client;

import com.vnpt.leakprocessor.dto.CtipLeakResponse;
import java.time.LocalDate;
import java.util.List;

/**
 * Interface định nghĩa các phương thức kết nối giao tiếp với hệ thống cổng thông tin VNPT CTIP.
 */
public interface CtipClient {
    
    /**
     * Tải danh sách thông tin tài khoản bị lộ lọt từ API CTIP.
     *
     * @param credentialSearch Từ khóa tìm kiếm hoặc tên miền (ví dụ: "ausca.vnpt.vn")
     * @param createdAtGte Lọc ngày ghi nhận từ (lớn hơn hoặc bằng)
     * @param createdAtLte Lọc ngày ghi nhận đến (nhỏ hơn hoặc bằng)
     * @param page Số trang cần lấy dữ liệu (bắt đầu từ trang 1)
     * @param size Số lượng bản ghi tối đa trả về trên mỗi trang (tối đa 100)
     * @return Đối tượng CtipLeakResponse chứa kết quả phản hồi và thông tin phân trang.
     */
    CtipLeakResponse fetchLeaks(String credentialSearch, LocalDate createdAtGte, LocalDate createdAtLte, int page, int size);

    /**
     * Cập nhật trạng thái đóng các sự cố lộ lọt tài khoản trên hệ thống CTIP.
     *
     * @param ids Danh sách status_id của các sự cố cần cập nhật
     * @param status Trạng thái mới cần thiết lập (ví dụ: "close")
     */
    void updateCtipStatus(List<String> ids, String status);
}
