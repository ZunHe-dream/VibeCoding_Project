package org.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.UserVisitor;
import org.example.mapper.UserVisitorMapper;
import org.example.service.UserVisitorService;
import org.springframework.stereotype.Service;

@Service
public class UserVisitorServiceImpl extends ServiceImpl<UserVisitorMapper, UserVisitor>
        implements UserVisitorService {}
