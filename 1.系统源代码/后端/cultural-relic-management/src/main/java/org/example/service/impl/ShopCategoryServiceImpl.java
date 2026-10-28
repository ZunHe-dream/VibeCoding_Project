package org.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.ShopCategory;
import org.example.mapper.ShopCategoryMapper;
import org.example.service.ShopCategoryService;
import org.springframework.stereotype.Service;

@Service
public class ShopCategoryServiceImpl extends ServiceImpl<ShopCategoryMapper, ShopCategory>
        implements ShopCategoryService {}