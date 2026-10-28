package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("exhibition_artifact_rel")
public class ExhibitionArtifactRel {
    @TableId(value = "rel_id", type = IdType.AUTO)
    private Long relId;
    private Long exhibitionId;
    private Long artifactId;
}