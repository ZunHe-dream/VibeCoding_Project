package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.ExhibitionApplication;

import java.util.List;
import java.util.Map;

public interface ExhibitionApplicationService extends IService<ExhibitionApplication> {
    // 审批方法：传入申请ID，状态(1或2)，以及备注
    boolean audit(Long appId, Integer status, String remark);
    List<Map<String, Object>> getArtifactListByBelong(String belong);
    void generateLoanRequests(ExhibitionApplication app);
}