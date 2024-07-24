package org.example.botwebsocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BotWebSocketApplication {

    public static void main(String[] args) {
        SpringApplication.run(BotWebSocketApplication.class, args);
    }

}
