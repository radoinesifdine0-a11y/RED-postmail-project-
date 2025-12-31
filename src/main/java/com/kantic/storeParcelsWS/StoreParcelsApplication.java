package com.kantic.storeParcelsWS;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Store Parcels Web Service.
 *
 * @SpringBootApplication combines 3 annotations:
 * - @Configuration: This class can define beans
 * - @EnableAutoConfiguration: Spring Boot configures things automatically
 * - @ComponentScan: Scan this package for @Controller, @Service, etc.
 */
@SpringBootApplication
public class StoreParcelsApplication {

    public static void main(String[] args) {
        // This starts the embedded Tomcat server and initializes Spring
        SpringApplication.run(StoreParcelsApplication.class, args);
    }
}
