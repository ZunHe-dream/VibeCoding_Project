package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("shop_cart")
public class ShopCart {
    @TableId(value = "cart_id", type = IdType.AUTO)
    private Long cartId;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private LocalDateTime createTime;
    private Long skuId;
    private String skuName;

    @TableField(exist = false)
    private ShopProduct product; // 联查商品信息
}