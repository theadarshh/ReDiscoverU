package com.rediscoveru.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * WebConfig — serves uploaded files as static resources.
 *
 * Any file saved to  {uploadDir}/...  is accessible at:
 *   http://localhost:8080/uploads/...
 *
 * Examples:
 *   uploads/mentors/mentor-1.jpg  → GET /uploads/mentors/mentor-1.jpg
 *   uploads/motivation/image.jpg  → GET /uploads/motivation/image.jpg
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload.dir:uploads}") private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Convert relative path to absolute file:// URI so Spring can find it
        // regardless of which directory the JVM was launched from.
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        if (!absolutePath.endsWith("/")) absolutePath += "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absolutePath)
                .setCacheControl(
                    org.springframework.http.CacheControl.noCache()   // always serve latest
                );
    }
}
