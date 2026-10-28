package org.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.ArtifactLoanRequest;
import org.example.mapper.ArtifactLoanRequestMapper;
import org.example.service.ArtifactLoanRequestService;
import org.springframework.stereotype.Service;

@Service
public class ArtifactLoanRequestServiceImpl
        extends ServiceImpl<ArtifactLoanRequestMapper, ArtifactLoanRequest>
        implements ArtifactLoanRequestService {}