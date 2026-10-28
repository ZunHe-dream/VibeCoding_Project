package org.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.PortalBanner;
import org.example.mapper.PortalBannerMapper;
import org.example.service.PortalBannerService;
import org.springframework.stereotype.Service;

@Service
public class BannerServiceImpl extends ServiceImpl<PortalBannerMapper, PortalBanner>
        implements PortalBannerService {}