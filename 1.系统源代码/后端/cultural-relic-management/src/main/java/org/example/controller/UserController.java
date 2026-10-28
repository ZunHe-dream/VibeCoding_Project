package org.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpSession;
import org.example.common.Result;
import org.example.entity.User;
import org.example.mapper.UserMapper;
import org.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 登录功能：增加 HttpSession 支持
     */
    @PostMapping("/login")
    public Result<User> login(@RequestBody User loginUser, HttpSession session) {
        // 调用业务层校验
        User user = userService.login(loginUser.getUsername(), loginUser.getPassword());

        if (user != null) {
            // ✨ 核心改进：登录成功，将用户信息存入 Session
            // 之后在 ExhibitionPlanController 里就可以通过 session.getAttribute("user") 拿到了
            session.setAttribute("user", user);

            return Result.success(user);
        } else {
            return Result.error("用户名或密码错误，请检查！");
        }
    }

    /**
     * 退出登录：清除 Session
     */
    @PostMapping("/logout")
    public Result logout(HttpSession session) {
        session.invalidate(); // 销毁 Session
        return Result.success("退出成功");
    }

//    // 2. 注册功能
//    @PostMapping("/register")
//    public Result<String> register(@RequestBody User user) {
//        boolean saved = userService.save(user);
//        return saved ? Result.success("注册成功") : Result.error("注册失败");
//    }

    // 3. 修改个人信息
    @PutMapping("/update")
    public Result<String> update(@RequestBody User user) {
        boolean updated = userService.updateById(user);
        return updated ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 修改密码
     */
    @PostMapping("/password")
    public Result updatePassword(@RequestBody Map<String, Object> params) {
        // 这里建议也改为从 Session 获取 userId 更加安全，防止越权
        Long userId = Long.valueOf(params.get("userId").toString());
        String oldPwd = (String) params.get("oldPassword");
        String newPwd = (String) params.get("newPassword");

        User user = userService.getById(userId);

        if (!user.getPassword().equals(oldPwd)) {
            return Result.error("原始密码错误，修改失败");
        }

        user.setPassword(newPwd);
        userService.updateById(user);

        return Result.success("密码修改成功");
    }

    @Autowired
    private UserMapper userMapper;

    // 查询本馆策展人员列表
    @GetMapping("/planners")
    public Result<List<User>> getPlanners(@RequestParam String belong) {
        List<User> list = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getBelong, belong)
                        .eq(User::getRole, "APPLICANT")
                        .orderByDesc(User::getCreateTime)
        );
        // 隐藏密码
        list.forEach(u -> u.setPassword(null));
        return Result.success(list);
    }

    // 新增/编辑策展人
    @PostMapping("/planner/save")
    public Result<String> savePlanner(@RequestBody User user) {
        // 新增时检查用户名是否重复
        if (user.getUserId() == null) {
            Long count = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())
            );
            if (count > 0) return Result.error("用户名已存在");
        }
        user.setRole("APPLICANT");
        return userService.saveOrUpdate(user) ? Result.success("保存成功") : Result.error("保存失败");
    }

    // 删除策展人
    @DeleteMapping("/planner/{id}")
    public Result<String> deletePlanner(@PathVariable Long id) {
        return userService.removeById(id) ? Result.success("删除成功") : Result.error("删除失败");
    }


    @PostMapping("/register")
    public Result<String> register(@RequestBody User user) {
        Long count = userService.count(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, user.getUsername())
        );
        if (count > 0) return Result.error("用户名已被占用");
        user.setRole("USER");
        return userService.save(user) ? Result.success("注册成功") : Result.error("注册失败");
    }

    // 全量用户列表
    @GetMapping("/super/list")
    public Result<List<User>> superList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String belong) {
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreateTime);
        if (StringUtils.hasText(keyword))
            w.like(User::getUsername, keyword).or().like(User::getNickname, keyword);
        if (StringUtils.hasText(role)) w.eq(User::getRole, role);
        if (StringUtils.hasText(belong)) w.like(User::getBelong, belong);
        List<User> list = userService.list(w);
        list.forEach(u -> u.setPassword(null));
        return Result.success(list);
    }

    // 新增/编辑用户
    @PostMapping("/super/save")
    public Result<String> superSave(@RequestBody User user) {
        if (user.getUserId() == null) {
            Long count = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername()));
            if (count > 0) return Result.error("用户名已存在");
        }
        if ("USER".equals(user.getRole())) user.setBelong("用户");
        return userService.saveOrUpdate(user) ? Result.success("保存成功") : Result.error("保存失败");
    }

    // 删除用户
    @DeleteMapping("/super/delete/{id}")
    public Result<String> superDelete(@PathVariable Long id) {
        return userService.removeById(id) ? Result.success("删除成功") : Result.error("删除失败");
    }
}