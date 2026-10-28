package org.example.controller;

import org.example.common.Result;
import org.example.entity.FavArtifact;
import org.example.service.IFavArtifactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/favorite")
public class FavArtifactController {

    // 注意：这里注入的是 Service 接口，而不是 Mapper
    @Autowired
    private IFavArtifactService favArtifactService;

    @GetMapping("/artifacts")
    public Result getMyArtifacts(@RequestParam Long userId) {
        // Controller 只负责接收参数并调用 Service，不写具体的 if-else 逻辑
        return Result.success(favArtifactService.getMyArtifactList(userId));
    }

    @PostMapping("/artifact/toggle")
    public Result toggleArtifact(@RequestBody FavArtifact fav) {
        String msg = favArtifactService.toggleFavorite(fav);
        return Result.success(msg);
    }
}
