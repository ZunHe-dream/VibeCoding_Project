package org.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.common.Result;
import org.example.entity.UserVisitor;
import org.example.service.UserVisitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/visitor")
public class UserVisitorController {

    @Autowired
    private UserVisitorService visitorService;

    @GetMapping("/list")
    public Result<List<UserVisitor>> list(@RequestParam Long userId) {
        return Result.success(visitorService.list(
                new LambdaQueryWrapper<UserVisitor>()
                        .eq(UserVisitor::getUserId, userId)
                        .orderByDesc(UserVisitor::getIsDefault)));
    }

    @PostMapping("/save")
    public Result<String> save(@RequestBody UserVisitor visitor) {
        if (visitor.getIsDefault() == 1) {
            UserVisitor update = new UserVisitor();
            update.setIsDefault(0);
            visitorService.update(update, new LambdaQueryWrapper<UserVisitor>()
                    .eq(UserVisitor::getUserId, visitor.getUserId()));
        }
        return visitorService.saveOrUpdate(visitor) ? Result.success("保存成功") : Result.error("保存失败");
    }

    @DeleteMapping("/{visitorId}")
    public Result<String> delete(@PathVariable Long visitorId) {
        return visitorService.removeById(visitorId) ? Result.success("删除成功") : Result.error("删除失败");
    }
}