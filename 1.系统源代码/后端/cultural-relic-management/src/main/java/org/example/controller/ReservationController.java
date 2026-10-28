package org.example.controller;

import org.example.common.Result;
import org.example.entity.ExhibitorReservationVO;
import org.example.entity.Reservation;
import org.example.entity.ReservationVO;
import org.example.service.ReservationService;
import org.example.service.impl.ReservationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reservation")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    /**
     * 1. 查询单条票据详情 (核心修复)
     * 返回 Result<ReservationVO>，确保 data 是 {} 对象而非 [] 数组
     */
    @GetMapping("/detail/{resId}")
    public Result<ReservationVO> getTicketDetail(@PathVariable Long resId) {
        // 关键：务必确认 getDetailById 返回的是单条 VO
        ReservationVO vo = reservationService.getDetailById(resId);
        if (vo == null) {
            return Result.error("未找到相关票据记录");
        }
        return Result.success(vo);
    }

    /**
     * 2. 我的预约列表 (这个返回数组是正确的)
     */
    @GetMapping("/my")
    public Result<List<ReservationVO>> getMyReservations(@RequestParam Long userId) {
        List<ReservationVO> list = reservationService.getMyDetails(userId);
        return Result.success(list);
    }

    // apply 和 cancel 方法代码保持不变...

    /**
     * 5. 修改预约联系信息
     */
    @PostMapping("/updateInfo")
    public Result<String> updateInfo(@RequestBody Reservation res) {
        // 基础校验
        if (res.getResId() == null) return Result.error("预约ID不能为空");

        // 只允许修改电话和邮件，不允许修改时间或展览ID以防作弊
        Reservation existing = reservationService.getById(res.getResId());
        if (existing == null) return Result.error("记录不存在");

        existing.setRealName(res.getRealName());
        existing.setPhone(res.getPhone());
        existing.setEmail(res.getEmail());

        boolean success = reservationService.updateById(existing);
        return success ? Result.success("修改成功") : Result.error("修改失败");
    }

    /**
     * 提交预约申请 - 完整版
     */
    @PostMapping("/apply")
    public Result<String> apply(@RequestBody Reservation res) {
        // 1. 核心必填项校验
        if (res.getUserId() == null) return Result.error("提交失败：用户信息缺失");
        if (res.getExhibitionId() == null) return Result.error("提交失败：展览信息缺失");
        if (res.getRealName() == null || res.getRealName().isEmpty()) return Result.error("请填写姓名");
        if (res.getPhone() == null || res.getPhone().isEmpty()) return Result.error("请填写联系电话");
        if (res.getReserveDate() == null) return Result.error("请选择预约日期");

        // 2. 补全业务逻辑字段
        // 状态：0-待核销 (匹配你数据库默认值)
        res.setStatus(0);

        // 核销码：必须生成，因为你数据库里 verify_code 是唯一索引 (UNI)
        // 生成一个8位大写随机码，如：A1B2C3D4
        String uniqueCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        res.setVerifyCode(uniqueCode);

        // 3. 执行入库
        // 这里的 res 对象里已经包含了前端传来的：
        boolean success = reservationService.save(res);

        if (success) {
            return Result.success("预约成功！核销码：" + uniqueCode);
        } else {
            return Result.error("数据库写入失败，请联系管理员");
        }
    }

    /**
     * 6. 巡展方查询预约名单 (新增)
     * 对应前端：/api/reservation/exhibitor/list
     */
    @GetMapping("/exhibitor/list")
    public Result<List<ExhibitorReservationVO>> getExhibitorList(@RequestParam String museumName) {
        List<ExhibitorReservationVO> list = reservationService.getListForExhibitor(museumName);
        return Result.success(list);
    }
    /**
     * 7. 巡展方核销预约 (新增)
     * 对应前端：/api/reservation/verify
     */
    @PostMapping("/verify")
    public Result<String> verify(@RequestBody Reservation res) {
        if (res.getResId() == null) return Result.error("预约ID不能为空");

        Reservation existing = reservationService.getById(res.getResId());
        if (existing == null) return Result.error("未找到该预约记录");
        if (existing.getStatus() != 0) return Result.error("该预约已核销或已取消");

        existing.setStatus(1); // 修改状态为：1-已核销/已参观
        boolean success = reservationService.updateById(existing);

        return success ? Result.success("核销成功") : Result.error("核销失败");
    }

    @GetMapping("/byExhibition")
    public Result<List<ReservationVO>> getByExhibition(@RequestParam Long exhibitionId) {
        return Result.success(reservationService.getByExhibitionId(exhibitionId));
    }

    /**
     * 取消预约接口
     * 前端请求路径: /reservation/cancel/2024019
     * 请求方式: POST (或者是 DELETE，根据你前端的需求决定，这里以 POST 为例)
     */
    @PostMapping("/cancel/{id}")
    public Result cancelReservation(@PathVariable("id") Long id) {
        // 调用 service 层修改状态
        boolean success = reservationService.cancelReservation(id);

        if (success) {
            return Result.success("取消预约成功"); // 这里的 Result 是你项目统一的返回体，没有的话可以用 Map 或 ResponseEntity
        } else {
            return Result.error("取消预约失败，预约单可能不存在或已核销");
        }
    }

}