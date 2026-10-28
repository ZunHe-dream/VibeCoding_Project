package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("artifact_loan_request")
public class ArtifactLoanRequest {
    @TableId(value = "req_id", type = IdType.AUTO)
    private Long reqId;
    private Long appId;
    private Long artifactId;
    private String artifactName;
    private String ownerMuseum;
    private Integer status; // 0-待审批 1-已同意 2-已拒绝
    private String auditRemark;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
    private Integer isFinalized; // 0-草稿 1-已签发
}