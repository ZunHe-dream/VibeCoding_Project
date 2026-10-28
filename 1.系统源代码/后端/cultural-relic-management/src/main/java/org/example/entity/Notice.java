package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notice") // 严格对应你的表名
public class Notice {

    @TableId(value = "notice_id", type = IdType.AUTO)
    private Long noticeId;

    private String title;

    private String content; // LONGTEXT 在 Java 中直接用 String 即可

    private Long authorId;

    private String authorName;

    private String noticeType;

    private Integer isTop; // 是否置顶: 0-否, 1-是

    private LocalDateTime createTime;

    @TableField(exist = false)
    private String authorBelong;
}