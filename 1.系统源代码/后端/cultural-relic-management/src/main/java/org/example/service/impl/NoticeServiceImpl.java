package org.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.Notice;
import org.example.mapper.NoticeMapper;
import org.example.service.NoticeService;
import org.springframework.stereotype.Service;

@Service // 必须加！否则 Controller 无法通过 @Autowired 注入它
public class NoticeServiceImpl
        extends ServiceImpl<NoticeMapper, Notice> // 绑定对应的 Mapper 和 实体类
        implements NoticeService {                // 实现对应的 Service 接口

    // 这里目前不需要写代码，ServiceImpl 已经帮你实现好了所有的增删改查逻辑
}