package org.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.ShopOrderItem;
import org.example.mapper.ShopOrderItemMapper;
import org.example.service.ShopOrderItemService;
import org.springframework.stereotype.Service;

@Service
public class ShopOrderItemServiceImpl extends ServiceImpl<ShopOrderItemMapper, ShopOrderItem>
        implements ShopOrderItemService{}