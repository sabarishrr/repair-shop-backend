package com.repairshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
public class RepairShopApplication {

    @PostConstruct
    public void init() {
        // Set JVM default TimeZone to Indian Standard Time (IST)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    public static void main(String[] args) {
        SpringApplication.run(RepairShopApplication.class, args);
    }
}
