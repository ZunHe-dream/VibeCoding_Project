package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("reservation")
public class Reservation {
    @TableId(value = "res_id", type = IdType.AUTO) // 修正主键名
    private Long resId;

    private Long userId;

    private Long exhibitionId; // 关联 exhibition_plan 的主键

    private LocalDate reserveDate; // 修正日期字段名

    private Integer status; // 0-已预约, 1-已核销, 2-已取消

    // ✨ 新增字段对应数据库
    private String timeSlot;     // 参观场次
    private String ticketType;   // 票种
    private String verifyCode;   // 唯一核销码
    private String remark;       // 备注

    private String phone;
    private String email;

    private LocalDateTime createTime;

    private String realName;//真实姓名
    private String idCard; //身份证号码
}