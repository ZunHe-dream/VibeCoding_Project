package org.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.common.Result;
import org.example.entity.Notice;
import org.example.entity.NoticeType;
import org.example.entity.User;
import org.example.mapper.UserMapper;
import org.example.service.NoticeService;
import org.example.service.NoticeTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 公告管理控制器
 */
@RestController
@RequestMapping("/notice")
public class NoticeController {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private NoticeTypeService noticeTypeService;
    @Autowired
    private NoticeService noticeService;

    /**
     * 1. 发布或修改公告
     * 逻辑：根据 noticeId 是否存在自动判断是 INSERT 还是 UPDATE
     */
    @PostMapping("/save")
    public Result<String> save(@RequestBody Notice notice) {
        // 兜底逻辑：如果是新发布的公告，确保关键字段不为空
        if (notice.getNoticeId() == null) {
            if (notice.getAuthorName() == null || notice.getAuthorName().isEmpty()) {
                notice.setAuthorName("管理员");
            }
            if (notice.getNoticeType() == null || notice.getNoticeType().isEmpty()) {
                notice.setNoticeType("系统公告");
            }
        }

        boolean success = noticeService.saveOrUpdate(notice);
        return success ? Result.success("公告操作成功") : Result.error("操作失败");
    }

    /**
     * 2. 查询公告列表 (用户端/管理端通用)
     * @param title 模糊搜索标题
     * @param type  精确筛选类型
     */
//    @GetMapping("/list")
//    public Result<List<Notice>> list(
//            @RequestParam(required = false) String title,
//            @RequestParam(required = false) String type) {
//
//        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
//        if (title != null && !title.isEmpty()) wrapper.like(Notice::getTitle, title);
//        if (type != null && !type.isEmpty()) wrapper.eq(Notice::getNoticeType, type);
//        wrapper.orderByDesc(Notice::getIsTop).orderByDesc(Notice::getCreateTime);
//
//        List<Notice> list = noticeService.list(wrapper);
//
//        // ✨ 补充每条公告的发布者所属单位
//        list.forEach(notice -> {
//            if (notice.getAuthorId() != null) {
//                User user = userMapper.selectById(notice.getAuthorId());
//                if (user != null) notice.setAuthorBelong(user.getBelong());
//            }
//        });
//
//        return Result.success(list);
//    }
    @GetMapping("/list")
    public Result<List<Notice>> list(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String authorBelong, // 新增参数
            @RequestParam(required = false) String type) {

        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        if (title != null && !title.isEmpty()) wrapper.like(Notice::getTitle, title);
        if (type != null && !type.isEmpty()) wrapper.eq(Notice::getNoticeType, type);
        wrapper.orderByDesc(Notice::getIsTop).orderByDesc(Notice::getCreateTime);

        List<Notice> list = noticeService.list(wrapper);

        // 补充单位信息并根据单位过滤
        List<Notice> resultList = list.stream().filter(notice -> {
            if (notice.getAuthorId() != null) {
                User user = userMapper.selectById(notice.getAuthorId());
                if (user != null) {
                    String belong = user.getBelong();
                    notice.setAuthorBelong(belong);
                    // 如果用户输入了搜索单位，则进行匹配过滤
                    if (authorBelong != null && !authorBelong.isEmpty()) {
                        return belong != null && belong.contains(authorBelong);
                    }
                }
            }
            // 如果没搜单位，保留所有记录；如果搜了但没匹配上，过滤掉
            return authorBelong == null || authorBelong.isEmpty();
        }).toList();

        return Result.success(resultList);
    }

    /**
     * 3. 根据ID获取单条公告详情
     */
    @GetMapping("/{id}")
    public Result<Notice> getById(@PathVariable Long id) {
        Notice notice = noticeService.getById(id);
        return notice != null ? Result.success(notice) : Result.error("公告不存在");
    }

    /**
     * 4. 删除公告
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        boolean removed = noticeService.removeById(id);
        return removed ? Result.success("删除成功") : Result.error("删除失败");
    }

    // 全站公告列表（跨机构）
    @GetMapping("/super/list")
    public Result<List<Notice>> superNoticeList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type) {
        LambdaQueryWrapper<Notice> w = new LambdaQueryWrapper<Notice>()
                .orderByDesc(Notice::getIsTop).orderByDesc(Notice::getCreateTime);
        if (StringUtils.hasText(keyword)) w.like(Notice::getTitle, keyword);
        if (StringUtils.hasText(type)) w.eq(Notice::getNoticeType, type);
        return Result.success(noticeService.list(w));
    }

    // 切换置顶
    @PostMapping("/toggleTop/{id}")
    public Result<String> toggleTop(@PathVariable Long id) {
        Notice notice = noticeService.getById(id);
        if (notice == null) return Result.error("公告不存在");
        notice.setIsTop(notice.getIsTop() == 1 ? 0 : 1);
        return noticeService.updateById(notice) ? Result.success("操作成功") : Result.error("操作失败");
    }

    @GetMapping("/types")
    public Result<List<String>> getTypes() {
        List<NoticeType> list = noticeTypeService.list(
                new LambdaQueryWrapper<NoticeType>().orderByAsc(NoticeType::getSort)
        );
        List<String> types = list.stream().map(NoticeType::getTypeName).collect(Collectors.toList());
        // 如果没有自定义类型，返回默认值
        if (types.isEmpty()) {
            types = Arrays.asList("系统公告", "博物馆公告", "巡展资讯", "活动通知", "温馨提示");
        }
        return Result.success(types);
    }
    @PostMapping("/types/add")
    public Result<String> addType(@RequestParam String typeName) {
        NoticeType t = new NoticeType();
        t.setTypeName(typeName);
        return noticeTypeService.save(t) ? Result.success("添加成功") : Result.error("添加失败");
    }

    @DeleteMapping("/types/{typeName}")
    public Result<String> deleteType(@PathVariable String typeName) {
        return noticeTypeService.remove(
                new LambdaQueryWrapper<NoticeType>().eq(NoticeType::getTypeName, typeName)
        ) ? Result.success("删除成功") : Result.error("删除失败");
    }
}
