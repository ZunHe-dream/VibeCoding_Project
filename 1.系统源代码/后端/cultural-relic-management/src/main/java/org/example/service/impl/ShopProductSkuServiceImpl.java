package org.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.ShopProductSku;
import org.example.mapper.ShopProductSkuMapper;
import org.example.service.ShopProductSkuService;
import org.springframework.stereotype.Service;

@Service
public class ShopProductSkuServiceImpl extends ServiceImpl<ShopProductSkuMapper, ShopProductSku>
        implements ShopProductSkuService {}