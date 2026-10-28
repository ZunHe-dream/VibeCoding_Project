package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("portal_banner")
public class PortalBanner {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String imageUrl;
    private String title;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
    private String mainTitle;
    private String linkUrl;
}