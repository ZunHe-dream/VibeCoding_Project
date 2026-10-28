package org.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.ExhibitionArtifactRel;
import org.example.mapper.ExhibitionArtifactRelMapper;
import org.example.service.ExhibitionArtifactRelService;
import org.springframework.stereotype.Service;


@Service
public class ExhibitionArtifactRelServiceImpl
        extends ServiceImpl<ExhibitionArtifactRelMapper, ExhibitionArtifactRel>
        implements ExhibitionArtifactRelService {}