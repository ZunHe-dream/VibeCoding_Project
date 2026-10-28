package org.example.entity; // 改成你自己的包名

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_fav_artifact")
public class FavArtifact {
    @TableId(type = IdType.AUTO)
    private Long favId;
    private Long userId;
    private Long artifactId;
    private LocalDateTime createTime;
}