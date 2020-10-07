package com.example.mechta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MechtaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MechtaApplication.class, args);
    }

}
