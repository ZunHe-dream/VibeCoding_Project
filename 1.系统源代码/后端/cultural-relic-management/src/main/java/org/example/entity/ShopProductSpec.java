package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("shop_product_spec")
public class ShopProductSpec {
    @TableId(value = "spec_id", type = IdType.AUTO)
    private Long specId;
    private Long productId;
    private String specName;
    private String specValues; // JSON数组字符串
}
