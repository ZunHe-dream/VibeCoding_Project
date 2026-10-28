package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.ExhibitionPlan;
import org.example.entity.Favorite;
import org.example.mapper.FavoriteMapper;
import org.example.service.ExhibitionPlanService;
import org.example.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    @Autowired
    private ExhibitionPlanService planService;

    @Override
    public List<ExhibitionPlan> getMyFavoriteExhibitions(Long userId) {
        // 1. 先查出该用户所有的收藏记录
        List<Favorite> favorites = this.list(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId));

        if (favorites.isEmpty()) return new ArrayList<>();

        // 2. 提取出所有的巡展ID
        List<Long> planIds = favorites.stream()
                .map(Favorite::getExhibitionId)
                .collect(Collectors.toList());

        // 3. 根据 ID 列表批量查询巡展详情
        return planService.listByIds(planIds);
    }
}
