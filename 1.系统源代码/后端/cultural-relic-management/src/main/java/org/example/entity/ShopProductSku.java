package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("shop_product_sku")
public class ShopProductSku {
    @TableId(value = "sku_id", type = IdType.AUTO)
    private Long skuId;
    private Long productId;
    private String skuName;
    private BigDecimal price;
    private Integer stock;
    private Integer stockWarning;
    private String coverUrl;
}