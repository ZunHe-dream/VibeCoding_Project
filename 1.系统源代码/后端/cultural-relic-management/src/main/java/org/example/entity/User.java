package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user") // 明确指定对应 sys_user 表
public class User {

    @TableId(value = "user_id", type = IdType.AUTO) // 指定主键字段名及自增策略
    private Long userId;

    private String username;

    private String password;

    private String nickname;

    /**
     * 角色: USER-普通用户, APPLICANT-申请方, ADMIN-管理员
     * 提示：Java中可以用String接收ENUM，MyBatis-Plus会自动转换
     */
    private String role;

    private String phone;

    private String email;

    private String avatar;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String belong;
}
