package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.*;
import org.example.mapper.ExhibitionApplicationMapper;
import org.example.mapper.UserMapper;
import org.example.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;



/**
 * 巡展申请业务实现类（专家审批增强版 - 已集成新字段同步）
 */
@Service
public class ExhibitionApplicationServiceImpl
        extends ServiceImpl<ExhibitionApplicationMapper, ExhibitionApplication>
        implements ExhibitionApplicationService {
    @Autowired
    private ArtifactLoanRequestService loanRequestService;

    @Autowired
    private ExhibitionArtifactRelService relService;

    @Autowired
    private ArtifactService artifactService;

    @Autowired
    private ExhibitionPlanService exhibitionPlanService;

    @Autowired
    private UserMapper userMapper;
    /**
     * 审批业务逻辑
     * 流程：校验状态 -> 更新申请单 -> [若通过] 批量锁定文物并更新位置 -> [若通过] 生成巡展计划
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean audit(Long appId, Integer status, String remark) {
        // 1. 获取申请单详情
        ExhibitionApplication app = this.getById(appId);
        User user = userMapper.selectById(app.getApplicantId());
        if (user != null) {
            app.setApplicantNickname(user.getNickname());
        }
        if (app == null) {
            throw new RuntimeException("审批失败：申请单 ID " + appId + " 不存在");
        }

        // 2. 状态前置检查：如果申请单已经处理过（不是0），则不允许重复操作
        if (app.getStatus() != 0) {
            throw new RuntimeException("审批失败：该申请单已处理过，请勿重复操作");
        }

        // 3. 如果是“通过”操作(status=1)，进行文物库存状态硬校验
        if (status == 1) {
            validateArtifactsAvailability(app.getArtifactIds());
        }

        // 4. 更新申请单状态
        app.setStatus(status);
        app.setAuditRemark(remark);
        app.setAuditTime(LocalDateTime.now());
        boolean appUpdated = this.updateById(app);

        // 5. 联动处理：仅在“审批通过”时执行
        if (appUpdated && status == 1) {
            // --- 联动 A：批量将文物状态标记为 1 (外借巡展中)，并更新当前位置 ---
            updateArtifactsToLoaned(app.getArtifactIds(), app.getTargetLocation());
            // --- 联动 B：自动生成【巡展计划】 ---
            ExhibitionPlan plan = generateExhibitionPlan(app); // ✨ 改为有返回值
            saveArtifactRel(plan.getExhibitionId(), app.getArtifactIds()); // ✨ 新增
        }

        return appUpdated;
    }

    /**
     * 辅助方法：校验所有申请借用的文物是否都处于“馆藏(0)”状态
     */
    private void validateArtifactsAvailability(String artifactIds) {
        if (!StringUtils.hasText(artifactIds)) return;

        String[] ids = artifactIds.split(",");
        for (String idStr : ids) {
            Long artifactId = Long.parseLong(idStr.trim());
            Artifact artifact = artifactService.getById(artifactId);

            if (artifact == null) {
                throw new RuntimeException("审批中断：文物ID [" + artifactId + "] 不存在");
            }
            if (artifact.getStatus() != 0) {
                String name = artifact.getArtifactName() != null ? artifact.getArtifactName() : "ID:" + artifactId;
                throw new RuntimeException("审批失败：文物 [" + name + "] 当前不可借用");
            }
        }
    }

    /**
     * 辅助方法：将申请的文物状态批量改为巡展中，并更新其实时位置
     */
    private void updateArtifactsToLoaned(String artifactIds, String targetLocation) {
        if (!StringUtils.hasText(artifactIds)) return;
        String[] ids = artifactIds.split(",");
        for (String idStr : ids) {
            Long artifactId = Long.parseLong(idStr.trim());
            Artifact artifact = artifactService.getById(artifactId);
            if (artifact != null) {
                artifact.setStatus(1);
                artifact.setCurrentLocation(targetLocation);
                artifactService.updateById(artifact);
            }
        }
    }

    /**
     * ✨ 核心改进：基于申请单生成巡展发布计划
     * 自动同步所有新字段：票价、限额、场次配置、封面图等
     */
    private ExhibitionPlan generateExhibitionPlan(ExhibitionApplication app) {
        ExhibitionPlan plan = new ExhibitionPlan();
        BeanUtils.copyProperties(app, plan);
        plan.setAppId(app.getAppId());
        plan.setLocation(app.getTargetLocation());
        plan.setDescription(app.getContent());
        plan.setCoverURL(app.getCoverUrl());
        if (app.getStartDate() != null) plan.setStartTime(app.getStartDate().atTime(9, 0));
        if (app.getEndDate() != null) plan.setEndTime(app.getEndDate().atTime(18, 0));
        plan.setCurrentStatus(0);
        plan.setCreateTime(LocalDateTime.now());
        exhibitionPlanService.save(plan);
        return plan; // ✨ 返回 plan（save 后 exhibitionId 会自动回填）
    }

    @Override
    public List<Map<String, Object>> getArtifactListByBelong(String belong) {
        return baseMapper.getArtifactListByBelong(belong);
    }

    private void saveArtifactRel(Long exhibitionId, String artifactIds) {
        if (!StringUtils.hasText(artifactIds)) return;
        String[] ids = artifactIds.split(",");
        for (String idStr : ids) {
            ExhibitionArtifactRel rel = new ExhibitionArtifactRel();
            rel.setExhibitionId(exhibitionId);
            rel.setArtifactId(Long.parseLong(idStr.trim()));
            relService.save(rel);
        }
    }

    public void generateLoanRequests(ExhibitionApplication app) {
        if (!StringUtils.hasText(app.getArtifactIds())) return;

        // ✨ 先删除该申请之前的所有子申请记录，再重新生成
        loanRequestService.remove(
                new LambdaQueryWrapper<ArtifactLoanRequest>()
                        .eq(ArtifactLoanRequest::getAppId, app.getAppId())
        );

        String[] ids = app.getArtifactIds().split(",");
        for (String idStr : ids) {
            Long artifactId = Long.parseLong(idStr.trim());
            Artifact artifact = artifactService.getById(artifactId);
            if (artifact == null) continue;

            ArtifactLoanRequest req = new ArtifactLoanRequest();
            req.setAppId(app.getAppId());
            req.setArtifactId(artifactId);
            req.setArtifactName(artifact.getArtifactName());
            req.setOwnerMuseum(artifact.getOwnerMuseum());
            req.setStatus(0);
            loanRequestService.save(req);
        }
    }

}
