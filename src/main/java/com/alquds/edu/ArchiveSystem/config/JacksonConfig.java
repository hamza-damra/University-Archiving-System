package com.alquds.edu.ArchiveSystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class JacksonConfig {
    
    @Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module module = new Hibernate6Module();
        // Disable lazy loading - don't serialize lazy-loaded properties
        module.disable(Hibernate6Module.Feature.USE_TRANSIENT_ANNOTATION);
        module.disable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        return module;
    }
    
    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder
                .modulesToInstall(hibernate6Module())
                .build();
    }
}
