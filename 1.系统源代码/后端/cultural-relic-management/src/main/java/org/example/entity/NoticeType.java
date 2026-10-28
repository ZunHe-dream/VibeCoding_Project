package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("notice_type")
public class NoticeType {
    @TableId(value = "type_id", type = IdType.AUTO)
    private Long typeId;
    private String typeName;
    private Integer sort;
}