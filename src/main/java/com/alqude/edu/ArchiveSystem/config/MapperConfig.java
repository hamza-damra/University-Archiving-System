package com.alqude.edu.ArchiveSystem.config;

import com.alqude.edu.ArchiveSystem.mapper.UserMapper;
import com.alqude.edu.ArchiveSystem.mapper.DocumentRequestMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {
    
    @Bean
    public UserMapper userMapper() {
        return Mappers.getMapper(UserMapper.class);
    }
    
    @Bean
    public DocumentRequestMapper documentRequestMapper() {
        return Mappers.getMapper(DocumentRequestMapper.class);
    }
}
