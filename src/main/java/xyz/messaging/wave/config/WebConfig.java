package xyz.messaging.wave.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @SuppressWarnings("deprecation")
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Spring Boot 3.x에서 슬래시 유무에 상관없이 매핑을 유연하게 매칭하도록 설정
        configurer.setUseTrailingSlashMatch(true);
    }
}
