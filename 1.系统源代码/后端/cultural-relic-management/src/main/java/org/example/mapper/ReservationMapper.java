package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.entity.ExhibitorReservationVO;
import org.example.entity.Reservation;
import org.example.entity.ReservationVO;

import java.util.List;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    /**
     * 1. 查询单条票据详情 (含展览、用户信息)
     */
    @Select("SELECT " +
            "r.res_id as resId, r.user_id as userId, r.exhibition_id as exhibitionId, " +
            "r.reserve_date as reserveDate, r.status, r.create_time as createTime, " +
            "r.time_slot as timeSlot, r.ticket_type as ticketType, r.verify_code as verifyCode, " +
            "e.title as exhibitionTitle, e.cover_url as coverImage, e.location, " +
            "r.id_card as idCard, " + // ✨ 关键：把身份证号查出来并起个别名
            "r.real_name as userName, u.phone " +
            "FROM reservation r " +
            "LEFT JOIN exhibition_plan e ON r.exhibition_id = e.exhibition_id " +
            "LEFT JOIN sys_user u ON r.user_id = u.user_id " +
            "WHERE r.res_id = #{resId}")
    ReservationVO getDetailWithInfo(@Param("resId") Long resId);

    /**
     * 2. 查询某个用户的预约列表 (含展览、用户信息)
     * 修复了之前 getMyDetails 查不到用户名的问题
     */
    @Select("SELECT " +
            "r.res_id as resId, r.user_id as userId, r.exhibition_id as exhibitionId, " +
            "r.reserve_date as reserveDate, r.status, r.create_time as createTime, " +
            "r.time_slot as timeSlot, r.ticket_type as ticketType, r.verify_code as verifyCode, " +
            "r.id_card as idCard, " + // ✨ 关键：把身份证号查出来并起个别名
            "e.title as exhibitionTitle, e.cover_url as coverImage, e.location, " +
            "r.real_name as userName, r.phone as phone " +
            "FROM reservation r " +
            "LEFT JOIN exhibition_plan e ON r.exhibition_id = e.exhibition_id " +
            "LEFT JOIN sys_user u ON r.user_id = u.user_id " +
            "WHERE r.user_id = #{userId} " +
            "ORDER BY r.create_time DESC")
    List<ReservationVO> getMyDetailsWithInfo(@Param("userId") Long userId);

        /**
         * 巡展方查询预约名单：直接查视图
         */
        @Select("SELECT * FROM view_exhibitor_reservation WHERE exhibitor_user_id = #{exhibitorId} ORDER BY create_time DESC")
        List<ExhibitorReservationVO> selectByExhibitor(@Param("exhibitorId") Long exhibitorId);

    @Select("SELECT * FROM view_exhibitor_reservation WHERE museum_name = #{museumName} ORDER BY create_time DESC")
    List<ExhibitorReservationVO> selectByMuseumName(@Param("museumName") String museumName);

    @Select("SELECT r.res_id as resId, r.real_name as userName, r.phone, r.id_card as idCard, " +
            "r.reserve_date as reserveDate, r.time_slot as timeSlot, r.ticket_type as ticketType, " +
            "r.status, r.verify_code as verifyCode, r.create_time as createTime " +
            "FROM reservation r " +
            "WHERE r.exhibition_id = #{exhibitionId} " +
            "ORDER BY r.reserve_date DESC")
    List<ReservationVO> getByExhibitionId(@Param("exhibitionId") Long exhibitionId);

}