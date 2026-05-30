package com.repairshop.config;

import com.repairshop.model.State;
import com.repairshop.repository.StateRepository;
import com.repairshop.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final AuthService authService;
    private final StateRepository stateRepository;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            authService.createAdminIfNotExists("admin", "admin123", "Shop Administrator");
            log.info("===================================================");
            log.info("  Default Admin: username=admin  password=admin123  ");
            log.info("===================================================");

            seedStates();
        };
    }

    private void seedStates() {
        if (stateRepository.count() == 0) {
            List<State> states = Arrays.asList(
                    new State("Jammu and Kashmir", "01"),
                    new State("Himachal Pradesh", "02"),
                    new State("Punjab", "03"),
                    new State("Chandigarh", "04"),
                    new State("Uttarakhand", "05"),
                    new State("Haryana", "06"),
                    new State("Delhi", "07"),
                    new State("Rajasthan", "08"),
                    new State("Uttar Pradesh", "09"),
                    new State("Bihar", "10"),
                    new State("Sikkim", "11"),
                    new State("Arunachal Pradesh", "12"),
                    new State("Nagaland", "13"),
                    new State("Manipur", "14"),
                    new State("Mizoram", "15"),
                    new State("Tripura", "16"),
                    new State("Meghalaya", "17"),
                    new State("Assam", "18"),
                    new State("West Bengal", "19"),
                    new State("Jharkhand", "20"),
                    new State("Odisha", "21"),
                    new State("Chhattisgarh", "22"),
                    new State("Madhya Pradesh", "23"),
                    new State("Gujarat", "24"),
                    new State("Daman and Diu", "25"),
                    new State("Dadra and Nagar Haveli", "26"),
                    new State("Maharashtra", "27"),
                    new State("Andhra Pradesh (Old)", "28"),
                    new State("Karnataka", "29"),
                    new State("Goa", "30"),
                    new State("Lakshadweep", "31"),
                    new State("Kerala", "32"),
                    new State("Tamil Nadu", "33"),
                    new State("Puducherry", "34"),
                    new State("Andaman and Nicobar Islands", "35"),
                    new State("Telangana", "36"),
                    new State("Andhra Pradesh", "37"),
                    new State("Ladakh", "38"));
            stateRepository.saveAll(states);
            log.info("States seeded successfully.");
        }
    }
}
