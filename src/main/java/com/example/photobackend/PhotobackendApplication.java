package com.example.photobackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PhotobackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhotobackendApplication.class, args);
    }
}
