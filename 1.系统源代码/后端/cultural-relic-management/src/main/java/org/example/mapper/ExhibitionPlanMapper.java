package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.entity.ExhibitionPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ExhibitionPlanMapper extends BaseMapper<ExhibitionPlan> {

    @Select("SELECT p.* FROM exhibition_plan p " +
            "INNER JOIN exhibition_application a ON p.app_id = a.app_id " +
            "WHERE a.applicant_id = #{applicantId} " +
            "ORDER BY p.exhibition_id DESC")
    List<ExhibitionPlan> selectPlansByApplicantId(Long applicantId);
}