package org.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.NoticeType;
import org.example.mapper.NoticeTypeMapper;
import org.example.service.NoticeTypeService;
import org.springframework.stereotype.Service;

@Service
public class NoticeTypeServiceImpl extends ServiceImpl<NoticeTypeMapper, NoticeType>
        implements NoticeTypeService {}