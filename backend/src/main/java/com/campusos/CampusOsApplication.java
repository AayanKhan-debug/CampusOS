package com.campusos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CampusOS — Student-focused campus platform.
 *
 * Entry point for the Spring Boot application.
 * @SpringBootApplication combines:
 *   - @Configuration      (this class is a source of bean definitions)
 *   - @EnableAutoConfiguration (Spring Boot auto-configures beans based on classpath)
 *   - @ComponentScan      (scans com.campusos.* for @Component, @Service, etc.)
 */
@SpringBootApplication
public class CampusOsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusOsApplication.class, args);
    }
}
