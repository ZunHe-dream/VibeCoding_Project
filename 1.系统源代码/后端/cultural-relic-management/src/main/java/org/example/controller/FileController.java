package org.example.controller;

import org.example.common.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class FileController {

    // 这里修改为你电脑上想存放图片的实际路径（注意文件夹要先手动创建好！）
    private static final String UPLOAD_PATH = "D:/museum_uploads/";

    @PostMapping("/image")
    public Result upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        // 1. 生成新文件名 (防止重复)
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString() + suffix;

        try {
            // 2. 确保目录存在
            File destDir = new File(UPLOAD_PATH);
            if (!destDir.exists()) destDir.mkdirs();

            // 3. 保存文件
            File destFile = new File(UPLOAD_PATH + newFileName);
            file.transferTo(destFile);

            // 4. 返回访问地址 (假设你后端跑在 8080)
            // 这里的 /files/ 是我们在下一步要映射的虚拟路径
            String url = "http://localhost:8080/files/" + newFileName;
            return Result.success(url);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败");
        }
    }
}
