package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("shop_product")
public class ShopProduct {
    @TableId(value = "product_id", type = IdType.AUTO)
    private Long productId;
    private Long catId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private Integer stockWarning;
    private String coverUrl;
    private String images;
    private Integer type;      // 0-实体 1-虚拟
    private Integer status;    // 0-下架 1-上架
    private String belongMuseum;
    private Long adminId;
    private Integer sales;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String catName;    // 联查分类名
    @TableField(exist = false)
    private Boolean lowStock;  // 是否库存预警

    private Integer isFreeShipping;      // 是否包邮
    private Integer hasShippingInsurance; // 是否含运费险

    @TableField(exist = false)
    private List<ShopProductSpec> specs; // 规格列表
    @TableField(exist = false)
    private List<ShopProductSku> skus;   // SKU列表
    private BigDecimal shippingFee;
    @TableField(exist = false)
    private Boolean hasMultiSku; // 是否多规格
}