package com.alqude.edu.ArchiveSystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.alqude.edu.ArchiveSystem.config.AcademicProperties;
import com.alqude.edu.ArchiveSystem.config.UploadProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
	UploadProperties.class,
	AcademicProperties.class
})
public class ArchiveSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArchiveSystemApplication.class, args);
	}

}
