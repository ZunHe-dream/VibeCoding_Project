package org.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.common.Result;
import org.example.entity.Artifact;
import org.example.entity.ExhibitionArtifactRel;
import org.example.entity.FavArtifact;
import org.example.mapper.ArtifactMapper;
import org.example.service.ArtifactService;
import org.example.service.ExhibitionArtifactRelService;
import org.example.service.FavoriteService;
import org.example.service.IFavArtifactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/artifact") // 访问路径的前缀
public class ArtifactController {

    @Autowired
    private IFavArtifactService favArtifactService;

    @Autowired
    private ExhibitionArtifactRelService relService;

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private ArtifactService artifactService;

    // 1. 新增或修改文物信息



    // 2. 分页及多条件查询 (按名称、朝代、类别筛选)
//    @GetMapping("/list")
//    public Result<List<Artifact>> list(
//            @RequestParam(required = false) String name,
//            @RequestParam(required = false) String dynasty,
//            @RequestParam(required = false) String category,
//            @RequestParam(required = false)String author
//            ) {
//
//        LambdaQueryWrapper<Artifact> wrapper = new LambdaQueryWrapper<>();
//        wrapper.like(name != null, Artifact::getArtifactName, name)
//                .eq(dynasty != null, Artifact::getDynasty, dynasty)
//                .eq(category != null, Artifact::getCategory, category)
//                .like(author != null,Artifact::getAuthor,author)
//                .orderByAsc(Artifact::getArtifactId);
//
//        return Result.success(artifactService.list(wrapper));
//    }
    @GetMapping("/list")
    public Result list(String category, String name,String ownerMuseum) {
        LambdaQueryWrapper<Artifact> wrapper = new LambdaQueryWrapper<>();
        // 1. 分类依然是硬性过滤
        wrapper.eq(StringUtils.hasText(ownerMuseum), Artifact::getOwnerMuseum, ownerMuseum);
        wrapper.eq(StringUtils.hasText(category), Artifact::getCategory, category);
        // 2. 只有当 name 真正有内容（不是 null 且不是 ""）时，才拼接 OR 模糊查询
        if (StringUtils.hasText(name)) {
            wrapper.and(qw -> qw
                    .like(Artifact::getArtifactName, name)
                    .or()
                    .like(Artifact::getAuthor, name)
                    .or()
                    .like(Artifact::getDynasty, name)
                    .or()
                    .like(Artifact::getStorageLocation, name)
            );
        }
        // 如果 name 为空，上面的 if 不执行，SQL 就只有 category 条件，自然查出全体数据
        wrapper.orderByAsc(Artifact::getArtifactId);
        return Result.success(artifactService.list(wrapper));
    }

    // 3. 删除文物
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        return artifactService.removeById(id) ? Result.success("删除成功") : Result.error("删除失败");
    }
    
    //4. 根据文物id获取单件文物的详细信息
    @GetMapping("/{id}")
    public Result<Artifact> getById(@PathVariable Integer id) {
        // 根据 ID 查询单条记录
        Artifact artifact = artifactService.getById(id);
        if (artifact == null) {
            return Result.error("文物不存在");
        }
        return Result.success(artifact);
    }


    /**
     * 文物外借监控统计
     * 返回本馆文物的借用次数、收藏次数排行
     */

    @GetMapping("/monitor")
    public Result<List<Map<String, Object>>> monitor(@RequestParam String ownerMuseum) {
        // 1. 查本馆所有文物
        LambdaQueryWrapper<Artifact> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(ownerMuseum), Artifact::getOwnerMuseum, ownerMuseum);
        List<Artifact> artifacts = artifactService.list(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Artifact art : artifacts) {
            Map<String, Object> item = new HashMap<>();
            item.put("artifactId", art.getArtifactId());
            item.put("artifactName", art.getArtifactName());
            item.put("category", art.getCategory());
            item.put("dynasty", art.getDynasty());
            item.put("imageUrl", art.getImageUrl());
            item.put("status", art.getStatus());
            item.put("storageLocation", art.getStorageLocation());
            item.put("author", art.getAuthor());

            // 2. 借用次数：查关联表
            long loanCount = relService.count(
                    new LambdaQueryWrapper<ExhibitionArtifactRel>()
                            .eq(ExhibitionArtifactRel::getArtifactId, art.getArtifactId())
            );
            item.put("loanCount", loanCount);

            // 3. 收藏次数：直接查 user_fav_artifact 表
            long favCount = favArtifactService.count(
                    new LambdaQueryWrapper<FavArtifact>()
                            .eq(FavArtifact::getArtifactId, art.getArtifactId())
            );
            item.put("favCount", favCount);

            result.add(item);
        }

        // 按借用次数降序
        result.sort((a, b) ->
                Long.compare((Long) b.get("loanCount"), (Long) a.get("loanCount"))
        );

        return Result.success(result);
    }

    // 全量文物列表
    @GetMapping("/super/list")
    public Result<List<Artifact>> superArtifactList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        LambdaQueryWrapper<Artifact> w = new LambdaQueryWrapper<Artifact>()
                .orderByDesc(Artifact::getCreateTime);
        if (StringUtils.hasText(keyword))
            w.like(Artifact::getArtifactName, keyword).or().like(Artifact::getOwnerMuseum, keyword);
        if (StringUtils.hasText(category)) w.eq(Artifact::getCategory, category);
        return Result.success(artifactService.list(w));
    }

    // 1. 新增或修改文物信息
    @PostMapping("/save")
    public Result<String> save(@RequestBody Artifact artifact) {
        if (artifact.getArtifactId() == null) {
            artifact.setStatus(0);
        }
        return artifactService.saveOrUpdate(artifact) ? Result.success("保存成功") : Result.error("保存失败");
    }
}