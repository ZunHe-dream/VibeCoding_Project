package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.FavArtifact;
import org.example.mapper.FavArtifactMapper;
import org.example.service.IFavArtifactService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class FavArtifactServiceImpl extends ServiceImpl<FavArtifactMapper, FavArtifact> implements IFavArtifactService {

    @Override
    public List<Map<String, Object>> getMyArtifactList(Long userId) {
        // 调用 Mapper 执行 SQL
        return baseMapper.getMyArtifacts(userId);
    }

    @Override
    public String toggleFavorite(FavArtifact fav) {
        // 编写核心业务：检查 -> 判断 -> 执行
        QueryWrapper<FavArtifact> qw = new QueryWrapper<>();
        qw.eq("user_id", fav.getUserId()).eq("artifact_id", fav.getArtifactId());
        FavArtifact exist = baseMapper.selectOne(qw);

        if (exist != null) {
            baseMapper.deleteById(exist.getFavId());
            return "已取消收藏";
        } else {
            baseMapper.insert(fav);
            return "收藏成功";
        }
    }
}
