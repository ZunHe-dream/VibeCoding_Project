package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.ExhibitionPlan;
import org.example.entity.ExhibitorReservationVO;
import org.example.entity.Reservation;
import org.example.entity.ReservationVO;
import org.example.mapper.ReservationMapper;
import org.example.service.ExhibitionPlanService;
import org.example.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper, Reservation> implements ReservationService {

    @Autowired
    private ExhibitionPlanService planService;

    /**
     * 提交预约逻辑：保持不变，增加余票和重复性校验
     */
    @Transactional(rollbackFor = Exception.class)
    public String makeReservation(Reservation reservation) {
        // A. 校验人数上限
        ExhibitionPlan plan = planService.getById(reservation.getExhibitionId());
        if (plan == null) throw new RuntimeException("展览计划不存在");

        Long count = this.count(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getExhibitionId, reservation.getExhibitionId())
                .eq(Reservation::getReserveDate, reservation.getReserveDate())
                .in(Reservation::getStatus, 0, 1));

        if (count >= plan.getMaxCapacity()) {
            throw new RuntimeException("该日期预约名额已满");
        }

        // B. 校验重复预约 (仅校验状态为“已预约”的)
        Long hasReserved = this.count(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getExhibitionId, reservation.getExhibitionId())
                .eq(Reservation::getUserId, reservation.getUserId())
                .eq(Reservation::getReserveDate, reservation.getReserveDate())
                .eq(Reservation::getStatus, 0));

        if (hasReserved > 0) {
            throw new RuntimeException("您已预约过该场次，请勿重复操作");
        }

        this.save(reservation);
        return "预约成功";
    }

    /**
     * 获取我的预约列表：修复版
     * 直接调用 Mapper 进行多表联查，一次性带出所有字段
     */
    @Override
    public List<ReservationVO> getMyDetails(Long userId) {
        // 调用 baseMapper 中我们手写的联查 SQL
        return baseMapper.getMyDetailsWithInfo(userId);
    }

    /**
     * 获取详情：核心方法
     */
    @Override
    public ReservationVO getDetailById(Long resId) {
        // 强制调用 baseMapper 的单条查询方法
        // 确保这个方法的 SQL 语句最后没有多余的“返回多条”逻辑
        return baseMapper.getDetailWithInfo(resId);
    }

    @Override
    public List<ExhibitorReservationVO> getListForExhibitor(String museumName) {
        return baseMapper.selectByMuseumName(museumName);
    }

    @Override
    public List<ReservationVO> getByExhibitionId(Long exhibitionId) {
        return baseMapper.getByExhibitionId(exhibitionId);
    }

    @Override
    public boolean cancelReservation(Long id) {
        // 1. 健壮性检查（可选）：可以先查一下这笔预约是否存在，或者是否已经是已核销(1)状态
        Reservation reservation = this.getById(id);
        if (reservation == null) {
            return false; // 预约不存在
        }
        if (reservation.getStatus() == 1) {
            // 如果已经核销了，通常不允许取消
            return false;
        }

        // 2. 使用 MyBatis-Plus 更新状态为 2 (已取消)
        return this.lambdaUpdate()
                .eq(Reservation::getResId, id)  // 匹配主键 resId
                .set(Reservation::getStatus, 2) // 将 status 设为 2
                .update();
    }

}
