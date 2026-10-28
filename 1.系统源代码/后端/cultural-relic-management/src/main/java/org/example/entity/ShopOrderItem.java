package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shop_order_item")
public class ShopOrderItem {
    @TableId(value = "item_id", type = IdType.AUTO)
    private Long itemId;
    private String orderId;
    private Long productId;
    private String productName;
    private String coverUrl;
    private BigDecimal price;
    private Integer quantity;
    private String belongMuseum;
    private Integer itemStatus;
    private LocalDateTime shipTime;
    private LocalDateTime finishTime;
}