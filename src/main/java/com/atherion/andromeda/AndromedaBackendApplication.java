package com.atherion.andromeda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AndromedaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AndromedaBackendApplication.class, args);
    }

}
