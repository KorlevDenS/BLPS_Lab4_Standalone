package org.korolev.dens.ratingservice;

import org.korolev.dens.ratingservice.services.MessagesService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@ServletComponentScan
public class RatingServiceApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(RatingServiceApplication.class, args);
        MessagesService messagesService = context.getBean(MessagesService.class);
        messagesService.startListener();
    }

}
