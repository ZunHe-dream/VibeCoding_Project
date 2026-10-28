package org.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.ShopCart;
import org.example.mapper.ShopCartMapper;
import org.example.service.ShopCartService;
import org.springframework.stereotype.Service;

@Service
public class ShopCartServiceImpl extends ServiceImpl<ShopCartMapper, ShopCart>
        implements ShopCartService {}