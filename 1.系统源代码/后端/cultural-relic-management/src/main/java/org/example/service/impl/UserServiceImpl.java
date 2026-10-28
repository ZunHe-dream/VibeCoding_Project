package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.User;
import org.example.mapper.UserMapper;
import org.example.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User login(String username, String password) {
        // 构造查询条件：用户名相等 且 密码相等
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username)
                .eq(User::getPassword, password);
        // 执行查询，返回数据库里匹配的那一行数据
        return this.getOne(wrapper);
    }

    // 新增：注册逻辑
    @Override
    public boolean register(User user) {
        // 简单校验
        return this.save(user);
    }

    // 新增：更新逻辑
    @Override
    public boolean updateInfo(User user) {
        return this.updateById(user);
    }
}