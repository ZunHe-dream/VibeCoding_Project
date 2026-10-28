package org.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.Artifact;
import org.example.mapper.ArtifactMapper;
import org.example.service.ArtifactService;
import org.springframework.stereotype.Service;

@Service // 别忘了这个注解，否则 Controller 找不到它
public class ArtifactServiceImpl extends ServiceImpl<ArtifactMapper, Artifact> implements ArtifactService {
}