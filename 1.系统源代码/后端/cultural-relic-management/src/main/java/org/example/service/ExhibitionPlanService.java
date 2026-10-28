package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.ExhibitionPlan;

import java.util.List;

public interface ExhibitionPlanService extends IService<ExhibitionPlan> {
    List<ExhibitionPlan> getPlansByApplicant(Long applicantId);
    boolean confirmAndPublish(Long planId, Integer maxCapacity);
}