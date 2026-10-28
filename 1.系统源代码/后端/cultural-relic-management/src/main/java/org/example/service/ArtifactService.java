package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.Artifact;

public interface ArtifactService extends IService<Artifact> {
    // 这里以后可以写复杂的业务方法，比如：List<Artifact> findAvailableArtifacts();
}