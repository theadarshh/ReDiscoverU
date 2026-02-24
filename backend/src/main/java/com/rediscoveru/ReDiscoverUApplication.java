package com.rediscoveru;

import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ReDiscoverUApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReDiscoverUApplication.class, args);
    }
}
