package org.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.ExhibitionPlan;
import org.example.mapper.ExhibitionPlanMapper;
import org.example.service.ExhibitionPlanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExhibitionPlanServiceImpl
        extends ServiceImpl<ExhibitionPlanMapper, ExhibitionPlan>
        implements ExhibitionPlanService {

    @Override
    public List<ExhibitionPlan> getPlansByApplicant(Long applicantId) {
        // 调用 baseMapper 中定义的连表查询方法
        return baseMapper.selectPlansByApplicantId(applicantId);
    }

    /**
     * 实现确认发布逻辑
     */
    @Override
    @Transactional
    public boolean confirmAndPublish(Long exhibitionId, Integer maxCapacity) {
        if (exhibitionId == null) return false;

        ExhibitionPlan plan = this.getById(exhibitionId);
        if (plan == null) return false;

        // 如果传了新的人数，就用新的；否则保留审批时的原样
        if (maxCapacity != null) {
            plan.setMaxCapacity(maxCapacity);
        }
        plan.setCurrentStatus(1);

        return this.updateById(plan);
    }

}