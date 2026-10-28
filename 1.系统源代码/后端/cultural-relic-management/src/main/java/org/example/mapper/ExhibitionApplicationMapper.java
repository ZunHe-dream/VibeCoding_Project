package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.entity.ExhibitionApplication;

import java.util.List;
import java.util.Map;

public interface ExhibitionApplicationMapper extends BaseMapper<ExhibitionApplication> {
    @Select("SELECT " +
            "a.app_id as appId, " +
            "a.title as exhibitionTitle, " +
            "a.start_date as startDate, " +
            "a.end_date as endDate, " +
            "a.status as appStatus, " +
            "art.artifact_id as artifactId, " +
            "art.artifact_name as artifactName, " +
            "art.author, " +
            "art.category, " +
            "art.dynasty, " +
            "art.image_url as imageUrl, " +
            "art.status as artifactStatus, " +
            "art.storage_location as storageLocation, " +
            "art.current_location as currentLocation " +
            "FROM exhibition_application a " +
            "JOIN sys_user u ON a.applicant_id = u.user_id " +
            "JOIN artifact art ON FIND_IN_SET(art.artifact_id, REPLACE(a.artifact_ids, ', ', ',')) > 0 " +
            "WHERE u.belong = #{belong} AND a.status = 1 " +
            "ORDER BY a.app_id DESC")
    List<Map<String, Object>> getArtifactListByBelong(@Param("belong") String belong);

    @Select("SELECT " +
            "a.app_id as appId, a.title, a.target_location as targetLocation, " +
            "a.start_date as startDate, a.end_date as endDate, " +
            "a.status, a.audit_remark as auditRemark, a.create_time as createTime, " +
            "a.artifact_ids as artifactIds, a.ticket_types as ticketTypes, " +
            "a.time_slot_config as timeSlotConfig, " +
            "u.nickname as applicantNickname, u.belong as applicantBelong, " +
            "ep.exhibition_id as exhibitionId, ep.current_status as currentStatus " +
            "FROM exhibition_application a " +
            "LEFT JOIN sys_user u ON a.applicant_id = u.user_id " +
            "LEFT JOIN exhibition_plan ep ON ep.app_id = a.app_id " +
            "ORDER BY a.create_time DESC")
    List<Map<String, Object>> getAllExhibitionsForSuperAdmin();


}