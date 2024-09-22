package org.korolev.dens.blps_lab4_standalone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class BLPSLab4StandaloneApplication {

    public static void main(String[] args) {
        SpringApplication.run(BLPSLab4StandaloneApplication.class, args);
    }

}