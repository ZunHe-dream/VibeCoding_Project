package org.example.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.entity.Reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ReservationVO {
    private Long resId;
    private Long userId;
    private Long exhibitionId;
    private java.time.LocalDate reserveDate;
    private Integer status;
    private java.time.LocalDateTime createTime;

    // ✨ 为美化页面新增的字段
    private String exhibitionTitle; // 展览标题
    private String coverImage;      // 展览封面图 (对应数据库 e.cover_image)
    private String location;         // 展览地点
    private String timeSlot;         // 参观场次 (如 09:00-12:00)
    private String ticketType;       // 票种 (如 成人票)
    private String verifyCode;       // 核销唯一码
    private String userName;         // 用户姓名 (可从 sys_user 关联)
    private String phone;            // 联系电话
    private String idCard;
}



