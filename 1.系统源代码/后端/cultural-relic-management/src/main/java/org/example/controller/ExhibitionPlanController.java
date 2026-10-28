package org.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpSession;
import org.example.common.Result;
import org.example.entity.ExhibitionPlan;
import org.example.entity.User;
import org.example.service.ExhibitionPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/exhibition/plan")
public class ExhibitionPlanController {

    @Autowired
    private ExhibitionPlanService exhibitionPlanService;

    /**
     * 巡展搜索与筛选
     * 供【观众端】使用：必须过滤掉状态为 2 (已失效) 的记录
     */
    @GetMapping("/search")
    public Result<IPage<ExhibitionPlan>> search(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime
    ) {
        Page<ExhibitionPlan> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ExhibitionPlan> wrapper = new LambdaQueryWrapper<>();

        // ✨ 核心改动：强制排除已失效(status=2)的计划，观众不可见
        wrapper.ne(ExhibitionPlan::getCurrentStatus, 2);

        wrapper.like(StringUtils.hasText(title), ExhibitionPlan::getTitle, title);
        wrapper.like(StringUtils.hasText(location), ExhibitionPlan::getLocation, location);

        // 如果前端传了特定状态(0或1)，则按传的查，否则默认查非2的所有
        wrapper.eq(status != null, ExhibitionPlan::getCurrentStatus, status);

        if (StringUtils.hasText(beginTime)) {
            wrapper.ge(ExhibitionPlan::getEndTime, beginTime + " 00:00:00");
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le(ExhibitionPlan::getStartTime, endTime + " 23:59:59");
        }

        wrapper.orderByDesc(ExhibitionPlan::getExhibitionId);
        IPage<ExhibitionPlan> resultPage = exhibitionPlanService.page(page, wrapper);
        return Result.success(resultPage);
    }

    /**
     * 根据 ID 查询单条记录
     */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        ExhibitionPlan plan = exhibitionPlanService.getById(id);
        // ✨ 建议：如果查询到的 plan 状态为 2，可以在此处做拦截或返回特定标识
        return Result.success(plan);
    }

    /**
     * 获取当前单位名下的所有巡展计划
     * 供【合作单位】管理页使用
     */
    @GetMapping("/my-plans")
    public Result getMyPlans(@RequestParam Long userId) {
        List<ExhibitionPlan> list = exhibitionPlanService.list(
                new LambdaQueryWrapper<ExhibitionPlan>()
                        .exists("SELECT 1 FROM exhibition_application a WHERE a.app_id = exhibition_plan.app_id AND a.applicant_id = " + userId)
                        .orderByDesc(ExhibitionPlan::getExhibitionId)
        );
        return Result.success(list);
    }

    /**
     * 确认并发布展讯
     */
    // 修改 confirmPlan 方法
    @PostMapping("/confirm")
    public Result confirmPlan(@RequestBody ExhibitionPlan plan) {
        if (plan.getExhibitionId() == null) {
            return Result.error("计划ID不能为空");
        }

        // 这里不再只设那两个字段，而是根据传入的对象按需更新
        // 如果前端传了新的票价或须知，也能一起保存
        plan.setCurrentStatus(1); // 强制设为发布：状态改为 1

        boolean success = exhibitionPlanService.updateById(plan);
        return success ? Result.success("展讯发布成功") : Result.error("发布失败");
    }

}