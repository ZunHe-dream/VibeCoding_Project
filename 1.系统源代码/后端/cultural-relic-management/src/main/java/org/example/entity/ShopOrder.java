package org.example.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("shop_order")
public class ShopOrder {
    @TableId(value = "order_id")
    private String orderId;
    private Long userId;
    private BigDecimal totalPrice;
    private Integer status; // 0-待付款 1-待发货 2-待收货 3-已完成 4-已取消
    private Long addrId;
    private String receiver;
    private String phone;
    private String address;
    private String remark;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private List<ShopOrderItem> items;

    private String refundReason;
    private String refundRemark;
    private LocalDateTime refundTime;
    private Integer prevStatus;
}