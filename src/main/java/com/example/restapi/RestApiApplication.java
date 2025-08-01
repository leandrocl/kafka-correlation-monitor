package com.example.restapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;

@SpringBootApplication
@EnableScheduling
public class RestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestApiApplication.class, args);
    }

    @Configuration
    public class OpenApiConfig {
        
        @Bean
        public OpenAPI customOpenAPI() {
            return new OpenAPI()
                    .info(new Info()
                            .title("Spring Boot REST API")
                            .description("A RESTful API with Spring Boot, HSQLDB, and AWS SQS/SNS integration")
                            .version("1.0.0")
                            .contact(new Contact()
                                    .name("API Support")
                                    .email("support@example.com")));
        }
    }
} 