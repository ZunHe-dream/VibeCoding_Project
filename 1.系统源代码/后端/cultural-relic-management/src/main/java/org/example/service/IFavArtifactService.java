package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.FavArtifact;
import java.util.List;
import java.util.Map;

public interface IFavArtifactService extends IService<FavArtifact> {
    // 定义业务：获取收藏列表
    List<Map<String, Object>> getMyArtifactList(Long userId);

    // 定义业务：切换收藏状态（核心逻辑所在）
    String toggleFavorite(FavArtifact fav);
}
