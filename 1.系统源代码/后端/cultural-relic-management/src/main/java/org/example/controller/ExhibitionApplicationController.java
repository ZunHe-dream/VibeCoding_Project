package org.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.common.Result;
import org.example.entity.ExhibitionApplication;
import org.example.service.ExhibitionApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/exhibition/application")
public class ExhibitionApplicationController {
    @Autowired
    private ExhibitionApplicationService applicationService;

    // 1. 巡展方：提交/暂存申请
    @PostMapping("/submit")
    public Result<String> submit(@RequestBody ExhibitionApplication application) {
        return applicationService.saveOrUpdate(application) ?
                Result.success("申请已提交") : Result.error("提交失败");
    }

    // 2. 管理员：审批 (在这里触发同步到 Plan 的 Service)
    @PostMapping("/audit")
    public Result<String> audit(@RequestBody ExhibitionApplication app) {
        // 调用我们刚才讨论的那个带同步逻辑的 Service 方法
        boolean success = applicationService.audit(app.getAppId(), app.getStatus(), app.getAuditRemark());
        return success ? Result.success("审批操作已生效") : Result.error("操作失败");
    }

    // 3. 巡展方：查看自己的申请状态
    @GetMapping("/my-list/{userId}")
    public Result<List<ExhibitionApplication>> myList(@PathVariable Long userId) {
        return Result.success(applicationService.list(
                new LambdaQueryWrapper<ExhibitionApplication>()
                        .eq(ExhibitionApplication::getApplicantId, userId)
                        .orderByDesc(ExhibitionApplication::getCreateTime)
        ));
    }
}
