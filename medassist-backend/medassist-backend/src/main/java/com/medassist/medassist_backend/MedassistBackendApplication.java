package com.medassist.medassist_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.medassist.medassist_backend",
    "com.medassist.core"
})
@EnableJpaRepositories(basePackages = {
    "com.medassist.medassist_backend.repository",
    "com.medassist.core.repository"
})
@EntityScan(basePackages = {
    "com.medassist.medassist_backend.entity",
    "com.medassist.core.entity"
})
public class MedassistBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MedassistBackendApplication.class, args);
	}

}
