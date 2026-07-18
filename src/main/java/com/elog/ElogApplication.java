package com.elog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ElogApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElogApplication.class, args);
    }
}
