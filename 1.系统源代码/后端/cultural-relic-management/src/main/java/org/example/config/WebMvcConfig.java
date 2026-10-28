package org.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvc配置类：用于配置本地静态资源映射
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /**
         * 1. addResourceHandler("/files/**")
         * 表示：当浏览器访问地址中包含 "/files/" 时，触发此映射
         * * 2. addResourceLocations("file:D:/museum_uploads/")
         * 表示：将上面的访问映射到本地 D 盘的 museum_uploads 文件夹
         * 注意：路径最后的 "/" 一定不能少！
         */
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:D:/museum_uploads/");
    }
}