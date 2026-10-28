package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("artifact") // 对应数据库里的 artifact 表
public class Artifact {
    @TableId(value = "artifact_id", type = IdType.AUTO)
    private Long artifactId;
    // 对应字段 artifact_name
    private String artifactName;
    // 对应字段 category
    private String category;
    // 对应字段 dynasty
    private String dynasty;
    // 对应字段 description
    private String description;
    // 对应字段 image_url
    private String imageUrl;
    // 对应字段 status
    private Integer status;
    // 对应字段 storage_location
    private String storageLocation; // 原属馆
    private String currentLocation; // 当前位置
    // 对应字段 create_time
    // 对应字段 create_time
    @com.baomidou.mybatisplus.annotation.TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;

    private String author;
    private String ownerMuseum; // 所属博物馆

}