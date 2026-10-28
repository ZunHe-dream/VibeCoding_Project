package org.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.common.Result;
import org.example.entity.ArtifactLoanRequest;
import org.example.entity.ExhibitionApplication;
import org.example.service.ArtifactLoanRequestService;
import org.example.service.ExhibitionApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/loan")
public class ArtifactLoanController {

    @Autowired
    private ArtifactLoanRequestService loanRequestService;

    @Autowired
    private ExhibitionApplicationService applicationService;

    /**
     * 1. 管理员查看本馆待审批的文物借用子申请
     * 按 ownerMuseum 过滤
     */
    @GetMapping("/list")
    public Result<List<ArtifactLoanRequest>> list(@RequestParam String ownerMuseum,
                                                  @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<ArtifactLoanRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArtifactLoanRequest::getOwnerMuseum, ownerMuseum);
        if (status != null) wrapper.eq(ArtifactLoanRequest::getStatus, status);
        wrapper.orderByDesc(ArtifactLoanRequest::getCreateTime);
        return Result.success(loanRequestService.list(wrapper));
    }

    /**
     * 2. 管理员审批单件文物（同意/拒绝）
     */
    @PostMapping("/audit")
    public Result<String> audit(@RequestBody Map<String, Object> params) {
        Long reqId = Long.valueOf(params.get("reqId").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        String remark = params.getOrDefault("remark", "").toString();

        ArtifactLoanRequest req = loanRequestService.getById(reqId);
        if (req == null) return Result.error("记录不存在");
        if (req.getIsFinalized() == 1) return Result.error("已签发，不可修改"); // ✨ 加这行

        req.setStatus(status);
        req.setAuditRemark(remark);
        req.setAuditTime(LocalDateTime.now());
        loanRequestService.updateById(req);

        return Result.success(status == 1 ? "已同意借出" : "已拒绝借出");
    }

    /**
     * 3. 策展方查看某个申请的文物审批汇总
     * 返回每件文物的审批状态
     */
    @GetMapping("/summary")
    public Result<List<ArtifactLoanRequest>> summary(@RequestParam Long appId) {
        List<ArtifactLoanRequest> list = loanRequestService.list(
                new LambdaQueryWrapper<ArtifactLoanRequest>()
                        .eq(ArtifactLoanRequest::getAppId, appId)
                        .orderByAsc(ArtifactLoanRequest::getOwnerMuseum)
        );
        return Result.success(list);
    }

    /**
     * 4. 策展方确认办展
     * 只保留审批通过的文物，更新 exhibition_application 的 artifactIds
     * 然后触发正式生成巡展计划
     */
    @PostMapping("/confirm")
    public Result<String> confirm(@RequestParam Long appId) {
        // 检查是否还有待审批的
        long pendingCount = loanRequestService.count(
                new LambdaQueryWrapper<ArtifactLoanRequest>()
                        .eq(ArtifactLoanRequest::getAppId, appId)
                        .eq(ArtifactLoanRequest::getStatus, 0)
        );
        if (pendingCount > 0) return Result.error("还有 " + pendingCount + " 件文物尚未审批完成");

        // 拿到所有通过的文物ID
        List<ArtifactLoanRequest> approved = loanRequestService.list(
                new LambdaQueryWrapper<ArtifactLoanRequest>()
                        .eq(ArtifactLoanRequest::getAppId, appId)
                        .eq(ArtifactLoanRequest::getStatus, 1)
        );

        if (approved.isEmpty()) return Result.error("所有文物均未获批，无法办展");

        // 更新 artifactIds 只保留通过的
        String newArtifactIds = approved.stream()
                .map(r -> r.getArtifactId().toString())
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        ExhibitionApplication app = applicationService.getById(appId);
        if (app == null) return Result.error("申请不存在");
        app.setArtifactIds(newArtifactIds);
        applicationService.updateById(app);

        // 触发正式审批通过流程（生成巡展计划、锁定文物）
        applicationService.audit(appId, 1, "策展方确认办展，系统自动通过");

        return Result.success("办展确认成功，巡展计划已生成");
    }

    /**
     * 批量审批本馆文物（全部同意或全部拒绝）
     */
    @PostMapping("/batchAudit")
    public Result<String> batchAudit(@RequestBody Map<String, Object> params) {
        Integer status = Integer.valueOf(params.get("status").toString());
        String remark = params.getOrDefault("remark", "").toString();

        // 如果传了 reqIds 就按选中处理，否则按 ownerMuseum 全量（签发时自动拒绝用）
        if (params.containsKey("reqIds")) {
            List<Integer> reqIds = (List<Integer>) params.get("reqIds");
            reqIds.forEach(id -> {
                ArtifactLoanRequest req = loanRequestService.getById(id);
                if (req != null && req.getIsFinalized() == 0) {
                    req.setStatus(status);
                    req.setAuditRemark(remark);
                    req.setAuditTime(LocalDateTime.now());
                    loanRequestService.updateById(req);
                }
            });
        } else {
            // 原来的全量逻辑，签发时用
            String ownerMuseum = params.get("ownerMuseum").toString();
            Long appId = Long.valueOf(params.get("appId").toString());
            List<ArtifactLoanRequest> list = loanRequestService.list(
                    new LambdaQueryWrapper<ArtifactLoanRequest>()
                            .eq(ArtifactLoanRequest::getAppId, appId)
                            .eq(ArtifactLoanRequest::getOwnerMuseum, ownerMuseum)
                            .eq(ArtifactLoanRequest::getStatus, 0)
            );
            list.forEach(r -> {
                r.setStatus(status);
                r.setAuditRemark(remark);
                r.setAuditTime(LocalDateTime.now());
            });
            loanRequestService.updateBatchById(list);
        }
        return Result.success("操作成功");
    }

    @PostMapping("/finalize")
    public Result<String> finalize(@RequestParam Long appId, @RequestParam String ownerMuseum) {
        // 1. 锁定本馆文物
        List<ArtifactLoanRequest> list = loanRequestService.list(
                new LambdaQueryWrapper<ArtifactLoanRequest>()
                        .eq(ArtifactLoanRequest::getAppId, appId)
                        .eq(ArtifactLoanRequest::getOwnerMuseum, ownerMuseum)
        );
        list.forEach(r -> r.setIsFinalized(1));
        loanRequestService.updateBatchById(list);

        // 2. 检查该申请下是否还有未签发的博物馆
        long unFinalizedCount = loanRequestService.count(
                new LambdaQueryWrapper<ArtifactLoanRequest>()
                        .eq(ArtifactLoanRequest::getAppId, appId)
                        .eq(ArtifactLoanRequest::getIsFinalized, 0)
        );

        if (unFinalizedCount > 0) {
            return Result.success("本馆已签发，等待其他 " + unFinalizedCount + " 个博物馆签发中");
        }

        // 3. 所有博物馆都签发完了，不自动通过，等策展方确认
        return Result.success("所有博物馆均已签发，请通知策展方确认办展");
    }

}