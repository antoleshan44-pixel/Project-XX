package com.urbano.monolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.urbano.monolith",
        "com.urbano.common"
})
@EnableScheduling
public class UrbanoMonolithApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrbanoMonolithApplication.class, args);
    }
}