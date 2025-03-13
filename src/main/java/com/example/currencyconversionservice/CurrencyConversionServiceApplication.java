package com.example.currencyconversionservice;

import com.example.currencyconversionservice.model.User;
import com.example.currencyconversionservice.respository.UserRepository;
import com.example.currencyconversionservice.service.ApiKeyGenerator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class CurrencyConversionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CurrencyConversionServiceApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                User user = new User();
                user.setApiKey(ApiKeyGenerator.generateApiKey());
                user.setName("Test User");
                userRepository.save(user);
                System.out.println("Generated API Key: " + user.getApiKey());
            }
        };
    }
}