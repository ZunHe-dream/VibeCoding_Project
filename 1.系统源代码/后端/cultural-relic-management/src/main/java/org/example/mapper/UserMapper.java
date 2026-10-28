package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.entity.User;

public interface UserMapper extends BaseMapper<User> {
    // MyBatis-Plus 会自动帮我们实现基础的增删改查
}
