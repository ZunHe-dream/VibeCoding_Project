package org.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.common.Result;
import org.example.entity.PortalBanner;
import org.example.service.PortalBannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/banner")
public class BannerController {

    @Autowired
    private PortalBannerService bannerService;

    // 用户端：只查显示中的
    @GetMapping("/list")
    public Result<List<PortalBanner>> list() {
        return Result.success(bannerService.list(
                new LambdaQueryWrapper<PortalBanner>()
                        .eq(PortalBanner::getStatus, 1)
                        .orderByAsc(PortalBanner::getSort)
        ));
    }

    // 管理端：查全部
    @GetMapping("/admin/list")
    public Result<List<PortalBanner>> adminList() {
        return Result.success(bannerService.list(
                new LambdaQueryWrapper<PortalBanner>()
                        .orderByAsc(PortalBanner::getSort)
        ));
    }

    @PostMapping("/save")
    public Result<String> save(@RequestBody PortalBanner banner) {
        return bannerService.saveOrUpdate(banner) ? Result.success("保存成功") : Result.error("保存失败");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        return bannerService.removeById(id) ? Result.success("删除成功") : Result.error("删除失败");
    }

    @PostMapping("/sort")
    public Result<String> updateSort(@RequestBody List<PortalBanner> banners) {
        bannerService.updateBatchById(banners);
        return Result.success("排序已更新");
    }
}