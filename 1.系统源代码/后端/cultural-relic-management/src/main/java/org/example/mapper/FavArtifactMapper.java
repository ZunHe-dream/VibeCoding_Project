package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.entity.FavArtifact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface FavArtifactMapper extends BaseMapper<FavArtifact> {

    // 联合查询文物详细信息
    @Select("SELECT f.fav_id, f.artifact_id, a.artifact_name, a.image_url, a.dynasty, a.storage_location " +
            "FROM user_fav_artifact f " +
            "JOIN artifact a ON f.artifact_id = a.artifact_id " +
            "WHERE f.user_id = #{userId}")
    List<Map<String, Object>> getMyArtifacts(Long userId);
}