package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.Notice;

public interface NoticeService extends IService<Notice> {
    // 如果你有特殊的业务需求（比如发送邮件通知），可以在这里定义方法名
}