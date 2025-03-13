package com.example.currencyconversionservice.service;

import java.util.UUID;

public class ApiKeyGenerator {
    public static String generateApiKey() {
        return UUID.randomUUID().toString();
    }
}
