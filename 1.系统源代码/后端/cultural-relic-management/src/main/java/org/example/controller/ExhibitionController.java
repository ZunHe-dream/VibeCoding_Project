package org.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.common.Result;
import org.example.entity.*;
import org.example.mapper.ExhibitionApplicationMapper;
import org.example.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.example.entity.Favorite;
import org.example.service.FavoriteService;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/exhibition")
public class ExhibitionController {
    @Autowired
    private ArtifactLoanRequestService loanRequestService;

    @Autowired
    private ExhibitionApplicationService exhibitionService;

    @Autowired
    private ArtifactService artifactService;
    @Autowired
    private FavoriteService favoriteService;

    // ✨ 新增注入：处理已发布的计划
    @Autowired
    private ExhibitionPlanService exhibitionPlanService;

    @Autowired private ExhibitionApplicationMapper exhibitionApplicationMapper;
    @Autowired private ReservationService reservationService;
    @Autowired
    private ExhibitionArtifactRelService artifactRelService;
    /**
     * 1. 提交或修改申请
     * 修改逻辑：如果传入了 appId，则是修改，否则是新增
     */
//    @PostMapping("/apply")
//    public Result<String> apply(@RequestBody ExhibitionApplication application) {
//        application.setStatus(0); // 只要提交或修改，状态强制回滚为待审批
//        boolean savedOrUpdated = exhibitionService.saveOrUpdate(application);
//        return savedOrUpdated ? Result.success("申请已提交，请等待审批") : Result.error("提交失败");
//    }
    @PostMapping("/apply")
    public Result<String> apply(@RequestBody ExhibitionApplication application) {
        application.setStatus(0);
        boolean savedOrUpdated = exhibitionService.saveOrUpdate(application);
        if (savedOrUpdated && application.getAppId() != null
                && StringUtils.hasText(application.getArtifactIds())) {
            // ✨ 新增：生成文物借用子申请
            exhibitionService.generateLoanRequests(application);
        }
        return savedOrUpdated ? Result.success("申请已提交") : Result.error("提交失败");
    }

    /**
     * ✨ 核心新增：重新编辑逻辑 (撤回并进入重新审核)
     * 用于巡展单位在“展讯发布”页面发现信息有误时点击
     */
    @PostMapping("/re-edit")
    @Transactional
    public Result<String> reEdit(@RequestBody Map<String, Long> params) {
        Long exhibitionId = params.get("exhibitionId");
        Long appId = params.get("appId");

        if (appId == null) return Result.error("参数错误");

        // 1. 标记巡展计划为失效
        if (exhibitionId != null) {
            ExhibitionPlan plan = exhibitionPlanService.getById(exhibitionId);
            if (plan != null) {
                plan.setCurrentStatus(2);
                exhibitionPlanService.updateById(plan);
            }
        }

        // 2. 重置申请单状态
        ExhibitionApplication app = exhibitionService.getById(appId);
        if (app != null) {
            app.setStatus(0);
            exhibitionService.updateById(app);

            // ✅ 新增：释放文物状态，恢复为馆藏中(0)
            if (StringUtils.hasText(app.getArtifactIds())) {
                for (String idStr : app.getArtifactIds().split(",")) {
                    Artifact artifact = artifactService.getById(idStr.trim());
                    if (artifact != null) {
                        artifact.setStatus(0);
                        artifact.setCurrentLocation(artifact.getStorageLocation()); // 位置恢复为原馆藏地
                        artifactService.updateById(artifact);
                    }
                }
            }

            return Result.success("已成功撤回，原展讯已标记为失效");
        }

        return Result.error("未找到申请记录");
    }

    // 2. 管理员审批
    @PostMapping("/audit")
    public Result<String> audit(@RequestBody ExhibitionApplication app) {
        if (app.getAppId() == null || app.getStatus() == null) {
            return Result.error("审批失败：缺少必要参数");
        }
        try {
            boolean success = exhibitionService.audit(app.getAppId(), app.getStatus(), app.getAuditRemark());
            return success ? Result.success("审批操作成功") : Result.error("审批失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 3. 查看所有申请列表
    @GetMapping("/list")
    public Result<List<ExhibitionApplication>> list(@RequestParam(required = false) Long applicantId) {
        LambdaQueryWrapper<ExhibitionApplication> w = new LambdaQueryWrapper<ExhibitionApplication>()
                .orderByDesc(ExhibitionApplication::getCreateTime);
        if (applicantId != null) w.eq(ExhibitionApplication::getApplicantId, applicantId);
        return Result.success(exhibitionService.list(w));
    }

    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> getExhibitionDetail(@PathVariable Long id) {
        ExhibitionPlan plan = exhibitionPlanService.getById(id);
        if (plan == null) {
            return Result.error("未找到该巡展");
        }

        // ✨ 通过 plan 里的 appId 去查申请表，拿到 ticketTypes
        ExhibitionApplication app = exhibitionService.getById(plan.getAppId());

        Map<String, Object> resultMap = new HashMap<>();

        // ✨ 把 ticketTypes 补充进 plan 的返回里
        // 方案：用 Map 包装 plan 的数据，再塞入 ticketTypes
        Map<String, Object> planMap = new HashMap<>();
        planMap.put("exhibitionId", plan.getExhibitionId());
        planMap.put("appId", plan.getAppId());
        planMap.put("title", plan.getTitle());
        planMap.put("location", plan.getLocation());
        planMap.put("startTime", plan.getStartTime());
        planMap.put("endTime", plan.getEndTime());
        planMap.put("maxCapacity", plan.getMaxCapacity());
        planMap.put("currentStatus", plan.getCurrentStatus());
        planMap.put("timeSlotConfig", plan.getTimeSlotConfig());
        planMap.put("coverURL", plan.getCoverURL());
        planMap.put("bookingNotice", plan.getBookingNotice());
        // ✨ 关键：从申请表补充 ticketTypes
        planMap.put("ticketTypes", app != null ? app.getTicketTypes() : "[]");
        planMap.put("richContent", plan.getRichContent()); // ✨ 加这行

        // 查询借调文物列表
        List<Artifact> artifacts = new ArrayList<>();
        if (app != null && app.getArtifactIds() != null && !app.getArtifactIds().isEmpty()) {
            String[] ids = app.getArtifactIds().split(",");
            for (String idStr : ids) {
                Artifact artifact = artifactService.getById(idStr.trim());
                if (artifact != null) artifacts.add(artifact);
            }
        }
        resultMap.put("application", planMap);
        resultMap.put("artifacts", artifacts);
        resultMap.put("appDetail", app); // ✨ 新增这行

        return Result.success(resultMap);

    }

    // 专门用于审批页面的接口，查 exhibition_application 表
//    @GetMapping("/application/detail/{id}")
//    public Result<Map<String, Object>> getApplicationDetail(@PathVariable Long id) {
//        // 这里用回原来的 exhibitionService (对应申请表)
//        ExhibitionApplication app = exhibitionService.getById(id);
//
//        if (app == null) return Result.error("未找到申请详情");
//
//        Map<String, Object> resultMap = new HashMap<>();
//        resultMap.put("application", app);
//
//        // 处理关联文物
//        if (StringUtils.hasText(app.getArtifactIds())) {
//            String[] ids = app.getArtifactIds().split(",");
//            List<Artifact> artifacts = new ArrayList<>();
//            for (String idStr : ids) {
//                Artifact a = artifactService.getById(idStr.trim());
//                if (a != null) artifacts.add(a);
//            }
//            resultMap.put("artifacts", artifacts);
//        }
//        return Result.success(resultMap);
//    }

    // 5. 彻底删除/撤回申请
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id) {
        ExhibitionApplication app = exhibitionService.getById(id);
        if (app == null) return Result.error("申请不存在");
        if (app.getStatus() != 0) return Result.error("已在审批流程中，无法直接删除，请尝试申请变更");

        boolean removed = exhibitionService.removeById(id);
        return removed ? Result.success("申请已成功删除") : Result.error("删除失败");
    }

//    @GetMapping("/application/detail/{id}")
//    public Result getAppDetail(@PathVariable Long id) {
//        // 根据 appId 查询申请单原始信息
//        ExhibitionApplication app = exhibitionService.getById(id);
//        return Result.success(app);
//    }
@GetMapping("/application/detail/{id}")
public Result getAppDetail(@PathVariable Long id) {
    // 1. 查询申请单基础信息
    ExhibitionApplication app = exhibitionService.getById(id);
    if (app == null) return Result.error("未找到申请详情");

    // 2. 包装成前端需要的结构
    Map<String, Object> resultMap = new HashMap<>();
    resultMap.put("application", app); // 👈 对应前端的 res.data.data.application

    // 3. 处理文物列表 (如果不加这一段，前端 currentArtifacts 就会是空的)
    List<Artifact> artifacts = new ArrayList<>();
    if (StringUtils.hasText(app.getArtifactIds())) {
        String[] ids = app.getArtifactIds().split(",");
        for (String idStr : ids) {
            Artifact a = artifactService.getById(idStr.trim());
            if (a != null) artifacts.add(a);
        }
    }
    resultMap.put("artifacts", artifacts); // 👈 对应前端的 res.data.data.artifacts

    return Result.success(resultMap);
}

    @GetMapping("/artifact-list")
    public Result<List<Map<String, Object>>> getArtifactList(@RequestParam String belong) {
        List<Map<String, Object>> result = exhibitionService.getArtifactListByBelong(belong);
        return Result.success(result);
    }

    @GetMapping("/list/byMuseum")
    public Result<List<ExhibitionApplication>> listByMuseum(@RequestParam String ownerMuseum) {
        // 查有借用该博物馆文物的申请ID
        List<ArtifactLoanRequest> loanList = loanRequestService.list(
                new LambdaQueryWrapper<ArtifactLoanRequest>()
                        .eq(ArtifactLoanRequest::getOwnerMuseum, ownerMuseum)
        );

        if (loanList.isEmpty()) return Result.success(new ArrayList<>());

        // 提取不重复的 appId
        List<Long> appIds = loanList.stream()
                .map(ArtifactLoanRequest::getAppId)
                .distinct()
                .collect(Collectors.toList());

        // 查这些申请的详情
        List<ExhibitionApplication> apps = exhibitionService.listByIds(appIds);
        apps.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        return Result.success(apps);
    }

    @PostMapping("/plan/updateContent")
    public Result<String> updateContent(@RequestBody Map<String, Object> params) {
        Long exhibitionId = Long.valueOf(params.get("exhibitionId").toString());
        String richContent = params.get("richContent").toString();
        ExhibitionPlan plan = exhibitionPlanService.getById(exhibitionId);
        if (plan == null) return Result.error("巡展不存在");
        plan.setRichContent(richContent);
        // ✨ 加这行
        if (params.get("coverURL") != null) plan.setCoverURL(params.get("coverURL").toString());
        return exhibitionPlanService.updateById(plan) ? Result.success("发布成功") : Result.error("保存失败");
    }

    @GetMapping("/super/list")
    public Result<List<Map<String, Object>>> superList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer planStatus) {
        List<Map<String, Object>> list = exhibitionApplicationMapper.getAllExhibitionsForSuperAdmin();

        // 前端筛选（数据量不大时可在内存过滤）
        if (keyword != null && !keyword.isEmpty()) {
            list = list.stream().filter(m ->
                    String.valueOf(m.getOrDefault("title","")).contains(keyword) ||
                            String.valueOf(m.getOrDefault("applicantNickname","")).contains(keyword) ||
                            String.valueOf(m.getOrDefault("applicantBelong","")).contains(keyword)
            ).collect(Collectors.toList());
        }
        if (status != null) {
            list = list.stream().filter(m ->
                    Objects.equals(m.get("status"), status)
            ).collect(Collectors.toList());
        }
        if (planStatus != null) {
            list = list.stream().filter(m ->
                    Objects.equals(m.get("currentStatus"), planStatus)
            ).collect(Collectors.toList());
        }
        return Result.success(list);
    }

    // 逻辑删除展览（同步处理关联数据）
    @DeleteMapping("/super/delete/{appId}")
    @Transactional
    public Result<String> superDelete(@PathVariable Long appId) {
        ExhibitionApplication app = exhibitionService.getById(appId);
        if (app == null) return Result.error("申请不存在");

        // 1. 恢复文物状态
        if (StringUtils.hasText(app.getArtifactIds())) {
            for (String idStr : app.getArtifactIds().split(",")) {
                Artifact artifact = artifactService.getById(idStr.trim());
                if (artifact != null) {
                    artifact.setStatus(0);
                    artifact.setCurrentLocation(artifact.getStorageLocation());
                    artifactService.updateById(artifact);
                }
            }
        }

        // 2. 取消关联预约
        ExhibitionPlan plan = exhibitionPlanService.getOne(
                new LambdaQueryWrapper<ExhibitionPlan>().eq(ExhibitionPlan::getAppId, appId));
        if (plan != null) {
            Reservation cancelRes = new Reservation();
            cancelRes.setStatus(2);
            reservationService.update(cancelRes,
                    new LambdaQueryWrapper<Reservation>()
                            .eq(Reservation::getExhibitionId, plan.getExhibitionId())
                            .eq(Reservation::getStatus, 0));
            // 3. 标记计划失效
            plan.setCurrentStatus(2);
            exhibitionPlanService.updateById(plan);
        }

        // 4. 删除申请
        if (plan != null) {
            // 1. 先删文物借用子申请
            loanRequestService.remove(
                    new LambdaQueryWrapper<ArtifactLoanRequest>()
                            .eq(ArtifactLoanRequest::getAppId, appId)
            );
            // 2. 删展览文物关联中间表
            artifactRelService.remove(
                    new LambdaQueryWrapper<ExhibitionArtifactRel>()
                            .eq(ExhibitionArtifactRel::getExhibitionId, plan.getExhibitionId())
            );
            // 3. ✨ 先删除所有预约记录（reservation表有外键约束）
            reservationService.remove(
                    new LambdaQueryWrapper<Reservation>()
                            .eq(Reservation::getExhibitionId, plan.getExhibitionId())
            );
            // ✨ 4. 删除所有收藏记录
            favoriteService.remove(
                    new LambdaQueryWrapper<Favorite>()
                            .eq(Favorite::getExhibitionId, plan.getExhibitionId())
            );
            // 5. 删巡展计划
            exhibitionPlanService.removeById(plan.getExhibitionId());
        }
// 5. 最后删申请单
        exhibitionService.removeById(appId);
        return Result.success("已删除并同步处理关联数据");
    }
    @PostMapping("/plan/end")
    public Result<String> endExhibition(@RequestParam Long exhibitionId) {
        ExhibitionPlan plan = exhibitionPlanService.getById(exhibitionId);
        if (plan == null) return Result.error("巡展不存在");
        if (plan.getCurrentStatus() == 2) return Result.error("该巡展已结束");
        plan.setCurrentStatus(2);
        return exhibitionPlanService.updateById(plan) ? Result.success("巡展已结束") : Result.error("操作失败");
    }
}