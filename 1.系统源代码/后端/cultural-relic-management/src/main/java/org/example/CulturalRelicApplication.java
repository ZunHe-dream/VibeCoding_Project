package org.example; // 确保这里的包名和你文件夹路径一致

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.example.mapper") // 告诉 MyBatis 去哪里找你的数据库操作接口
public class CulturalRelicApplication {

    public static void main(String[] args) {
        SpringApplication.run(CulturalRelicApplication.class, args);
    }
}