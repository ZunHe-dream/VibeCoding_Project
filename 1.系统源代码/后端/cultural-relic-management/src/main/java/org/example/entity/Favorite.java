package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("favorite")
public class Favorite {
    @TableId(type = IdType.AUTO)
    private Long favId;

    private Long userId;       // 收藏者ID
    private Long exhibitionId; // 巡展计划ID (注意这里关联的是 exhibition_plan 的 ID)
    private LocalDateTime createTime;
}