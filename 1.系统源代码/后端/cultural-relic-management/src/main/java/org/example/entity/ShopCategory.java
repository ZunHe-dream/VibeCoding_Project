package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("shop_category")
public class ShopCategory {
    @TableId(value = "cat_id", type = IdType.AUTO)
    private Long catId;
    private String name;
    private String icon;
    private Integer sort;
}