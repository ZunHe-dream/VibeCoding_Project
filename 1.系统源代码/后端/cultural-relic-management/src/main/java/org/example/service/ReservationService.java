package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.Reservation;
import org.example.entity.ReservationVO;
import org.example.entity.ExhibitorReservationVO; // 确保导入了这个新 VO

import java.util.List;

public interface ReservationService extends IService<Reservation> {
    String makeReservation(Reservation reservation);
    List<ReservationVO> getMyDetails(Long userId);

    // ✨ 新增：获取单条票据详情
    ReservationVO getDetailById(Long resId);

    /**
     * ✨ 巡展方端新增：获取预约视图列表
     * 用于巡展方管理界面显示自己发布的巡展预约记录
     */
    List<ExhibitorReservationVO> getListForExhibitor(String museumName);
    List<ReservationVO> getByExhibitionId(Long exhibitionId);
    boolean cancelReservation(Long id);
}