package org.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.ShopProductSpec;
import org.example.mapper.ShopProductSpecMapper;
import org.example.service.ShopProductSpecService;
import org.springframework.stereotype.Service;

@Service
public class ShopProductSpecServiceImpl extends ServiceImpl<ShopProductSpecMapper, ShopProductSpec>
        implements ShopProductSpecService {}