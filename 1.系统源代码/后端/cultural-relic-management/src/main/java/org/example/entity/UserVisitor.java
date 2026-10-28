package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_visitor")
public class UserVisitor {
    @TableId(value = "visitor_id", type = IdType.AUTO)
    private Long visitorId;
    private Long userId;
    private String realName;
    private String phone;
    private String idCard;
    private String email;
    private Integer isDefault;
    private LocalDateTime createTime;
}