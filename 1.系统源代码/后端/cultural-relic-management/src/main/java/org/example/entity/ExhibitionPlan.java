package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exhibition_plan") // 对应巡展计划表
public class ExhibitionPlan {

    @TableId(value = "exhibition_id", type = IdType.AUTO)
    private Long exhibitionId;

    private Long appId;       // 关联的申请单ID

    private String title;     // 巡展标题

    private String location;  // 展出地点 (对应申请表的 targetLocation)

    private LocalDateTime startTime; // 开始时间

    private LocalDateTime endTime;   // 结束时间

    private Integer maxCapacity;     // 最大人数限制

    private Integer currentStatus;   // 状态：0-计划中，1-进行中，2-已结束

    private String description;      // 巡展描述 (对应申请表的 content)

    private LocalDateTime createTime;

    @TableField("cover_url")
    private String coverURL;

    // --- ✨ 新增补全：对应申请表新加的字段 ---

    private String timeSlotConfig;   // 场次名额配置 (JSON串)

    private BigDecimal ticketPrice;  // 票价

    private String bookingNotice;    // 预约须知

    private String richContent;


}
