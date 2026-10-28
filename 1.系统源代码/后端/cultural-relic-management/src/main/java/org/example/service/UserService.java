package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.User;

public interface UserService extends IService<User> {
    // 定义登录方法，返回 User 对象（包含权限信息）
    User login(String username, String password);
    boolean register(User user);
    boolean updateInfo(User user);
}