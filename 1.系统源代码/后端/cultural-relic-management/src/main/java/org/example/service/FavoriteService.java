package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.ExhibitionPlan;
import org.example.entity.Favorite;

import java.util.List;

public interface FavoriteService extends IService<Favorite> {
    // 获取我的收藏列表（包含巡展的详细信息）
    List<ExhibitionPlan> getMyFavoriteExhibitions(Long userId);
}