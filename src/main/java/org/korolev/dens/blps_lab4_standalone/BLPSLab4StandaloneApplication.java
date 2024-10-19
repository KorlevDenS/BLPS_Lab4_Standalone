package org.korolev.dens.blps_lab4_standalone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
@ServletComponentScan
public class BLPSLab4StandaloneApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(BLPSLab4StandaloneApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(BLPSLab4StandaloneApplication.class, args);
    }

}