package org.example.controller;

import org.example.common.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/file") // 全局唯一的上传入口
public class CommonFileController {

    // 建议在配置文件或这里定义好存储根目录
    private final String uploadPath = "D:/museum_uploads/";

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return Result.error("文件为空");

        // 1. 确保目录存在
        File dir = new File(uploadPath);
        if (!dir.exists()) dir.mkdirs();

        // 2. 保持后缀，生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID() + suffix;

        // 3. 物理保存
        try {
            file.transferTo(new File(dir, newFileName));
        } catch (IOException e) {
            return Result.error("上传失败：" + e.getMessage());
        }

        // 4. 返回相对路径（配合 WebMvcConfig 预览）
        return Result.success("/files/" + newFileName);
    }
}