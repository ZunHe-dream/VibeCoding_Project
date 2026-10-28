package org.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.common.Result;
import org.example.entity.Favorite;
import org.example.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * 1. 切换收藏状态
     */
    @PostMapping("/toggle")
    public Result<String> toggle(@RequestBody Favorite favorite) {
        LambdaQueryWrapper<Favorite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Favorite::getUserId, favorite.getUserId())
                .eq(Favorite::getExhibitionId, favorite.getExhibitionId());

        // 使用 getOne 时，第二个参数传 false，代表如果有多个只取第一个，不报错
        Favorite existing = favoriteService.getOne(queryWrapper, false);

        if (existing != null) {
            boolean removed = favoriteService.removeById(existing.getFavId());
            return removed ? Result.success("已取消收藏") : Result.error("取消失败");
        } else {
            // 这里可以手动设置一下收藏时间
            // favorite.setCreateTime(LocalDateTime.now());
            boolean saved = favoriteService.save(favorite);
            return saved ? Result.success("收藏成功") : Result.error("收藏失败");
        }
    }

    /**
     * 2. 检查用户是否已收藏该巡展
     * 用于前端详情页“爱心”图标的高亮显示
     */
    @GetMapping("/check")
    public Result<Boolean> check(@RequestParam Long userId, @RequestParam Long exhibitionId) {
        long count = favoriteService.count(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getExhibitionId, exhibitionId));
        return Result.success(count > 0);
    }

    /**
     * 3. 获取我的收藏夹列表
     * 逻辑：根据用户ID查询所有收藏的巡展详细信息
     */
    @GetMapping("/my")
    public Result<List<org.example.entity.ExhibitionPlan>> getMyFavorites(@RequestParam Long userId) {
        // 调用 Service 层我们之前写好的关联查询逻辑
        return Result.success(favoriteService.getMyFavoriteExhibitions(userId));
    }
}