package org.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.ShopReview;
import org.example.mapper.ShopReviewMapper;
import org.example.service.ShopReviewService;
import org.springframework.stereotype.Service;

@Service
public class ShopReviewServiceImpl extends ServiceImpl<ShopReviewMapper, ShopReview>
        implements ShopReviewService {}