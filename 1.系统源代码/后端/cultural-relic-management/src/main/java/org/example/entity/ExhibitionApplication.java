package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("exhibition_application")
public class ExhibitionApplication {

    @TableId(value = "app_id", type = IdType.AUTO)
    private Long appId;

    private Long applicantId;      // 申请人ID
    private String title;          // 巡展主题
    private String content;        // 方案描述/展览介绍
    private String targetLocation; // 对应 SQL 中的 target_location (巡展地点)
    private String coverUrl;       // 巡展封面图地址

    // --- 核心时间与名额配置 ---
    private LocalDate startDate;   // 预计开始日期
    private LocalDate endDate;     // 预计结束日期
    private Integer maxCapacity;   // 每日总限额 (对应计划表的 max_capacity)

    // --- ✨ 新增：预约进阶配置 (对应 SQL 补丁) ---
    private String timeSlotConfig;  // 场次名额配置 (JSON串，如：{"上午":80, "下午":20})
    private BigDecimal ticketPrice; // 票价
    private String bookingNotice;   // 预约须知 (用户预约前看到的文字)

    // --- 审批与状态 ---
    private String artifactIds;    // 借用的文物ID串 (如 "1,2,3")
    private Integer status;        // 0-待审批, 1-已通过, 2-已驳回
    private String auditRemark;    // 审批备注/驳回理由
    private LocalDateTime auditTime; // 审批时间
    private LocalDateTime createTime; // 提交时间

    @TableField("ticket_types")
    private String ticketTypes;

    @TableField(exist = false)  // 不是数据库字段，只用于返回给前端
    private String applicantNickname;

}
