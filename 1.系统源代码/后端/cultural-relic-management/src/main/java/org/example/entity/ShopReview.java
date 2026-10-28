package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("shop_review")
public class ShopReview {
    @TableId(value = "review_id", type = IdType.AUTO)
    private Long reviewId;
    private String orderId;
    private Long productId;
    private Long userId;
    private Integer rating;
    private String content;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String userName;
}