package com.example.currencyconversionservice.service;

import com.example.currencyconversionservice.model.User;
import com.example.currencyconversionservice.respository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ApiKeyService {
    private final UserRepository userRepository;

    public ApiKeyService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isValidApiKey(String apiKey) {
        Optional<User> user = userRepository.findByApiKey(apiKey);
        return user.isPresent();
    }
}
