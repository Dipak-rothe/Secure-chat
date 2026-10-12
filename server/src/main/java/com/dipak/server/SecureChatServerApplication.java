package com.dipak.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SecureChatServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecureChatServerApplication.class, args);
    }
}
