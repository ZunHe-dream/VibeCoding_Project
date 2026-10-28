package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.entity.Notice;
import org.apache.ibatis.annotations.Mapper;

@Mapper // 告诉 Spring 这是一个映射器
public interface NoticeMapper extends BaseMapper<Notice> {
    // 这里不需要写任何代码，BaseMapper 已经自带了 insert, delete, update, selectById 等方法
}