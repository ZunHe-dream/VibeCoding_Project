package org.example.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("view_exhibitor_reservation") // 指向视图名称
public class ExhibitorReservationVO {
    private Long resId;
    private String realName;
    private String phone;
    private String exhibitionTitle;
    private LocalDate reserveDate;
    private Integer status;
    private String timeSlot;
    private String ticketType;
    private String idCard; // ✨ 确保这里叫 idCard
    private String verifyCode;
    private String remark;
    private String museumName;

    // 关键过滤字段：当前登录的巡展方ID
    private Long exhibitorUserId;
}