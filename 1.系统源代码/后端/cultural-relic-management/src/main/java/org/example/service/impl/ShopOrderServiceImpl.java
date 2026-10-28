package org.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.ShopOrder;
import org.example.mapper.ShopOrderMapper;
import org.example.service.ShopOrderService;
import org.springframework.stereotype.Service;

@Service
public class ShopOrderServiceImpl extends ServiceImpl<ShopOrderMapper, ShopOrder>
        implements ShopOrderService {}