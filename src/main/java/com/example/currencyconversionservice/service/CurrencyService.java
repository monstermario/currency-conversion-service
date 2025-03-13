package com.example.currencyconversionservice.service;

import com.example.currencyconversionservice.model.RequestLog;
import com.example.currencyconversionservice.model.User;
import com.example.currencyconversionservice.respository.RequestLogRepository;
import com.example.currencyconversionservice.respository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CurrencyService {

    private final RestTemplate restTemplate;
    private final RequestLogRepository logRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;


    @Value("${openexchangerates.api.url}")
    String exchangeApiUrl;

    @Value("${openexchangerates.api.key}")
    String appId;

    public CurrencyService(RestTemplate restTemplate, RequestLogRepository logRepository, UserRepository userRepository, StringRedisTemplate redisTemplate) {
        this.restTemplate = restTemplate;
        this.logRepository = logRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    public Double convertCurrency(String apiKey, String from, String to, Double amount) {
        Optional<User> user = userRepository.findByApiKey(apiKey);
        if (user.isEmpty()) {
            throw new RuntimeException("Invalid API Key");
        }

        enforceRequestLimits(apiKey);

        String conversionCacheKey = String.format("conversion:%s:%s:%.2f", from, to, amount);
        String cachedConvertedAmount = redisTemplate.opsForValue().get(conversionCacheKey);
        if (cachedConvertedAmount != null) {
            return Double.parseDouble(cachedConvertedAmount);
        }

        Double conversionRate = getCachedExchangeRate(from, to);

        Double convertedAmount = amount * conversionRate;

        redisTemplate.opsForValue().set(conversionCacheKey, String.valueOf(convertedAmount), 5, TimeUnit.MINUTES);

        RequestLog log = new RequestLog(null, apiKey, from, to, amount, convertedAmount, LocalDateTime.now());
        logRepository.save(log);

        return convertedAmount;
    }

    public boolean isValidCurrency(String currency) {
        String cacheKey = "validCurrencies";
        Set<String> validCurrencies = redisTemplate.opsForSet().members(cacheKey);

        if (validCurrencies == null || validCurrencies.isEmpty()) {
            validCurrencies = fetchValidCurrencies();
            redisTemplate.opsForSet().add(cacheKey, validCurrencies.toArray(new String[0]));
            redisTemplate.expire(cacheKey, 1, TimeUnit.HOURS); // Cache for 1 hour
        }

        return validCurrencies.contains(currency.toUpperCase());
    }

    public Set<String> fetchValidCurrencies() {
        String url = exchangeApiUrl + "?app_id=" + appId;

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null || !response.containsKey("rates")) {
            throw new RuntimeException("Failed to fetch currency codes.");
        }

        Map<String, Object> rates = (Map<String, Object>) response.get("rates");
        return rates.keySet().stream().map(String::toUpperCase).collect(Collectors.toSet());
    }

    private void enforceRequestLimits(String apiKey) {
        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(2);
        List<RequestLog> recentRequests = logRepository.findByApiKeyAndTimestampAfter(apiKey, twoMinutesAgo);
        if (!recentRequests.isEmpty()) {
            throw new RuntimeException("You must wait at least 2 minutes before making another request.");
        }

        DayOfWeek today = LocalDateTime.now().getDayOfWeek();
        int requestLimit = (today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY) ? 200 : 100;

        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        List<RequestLog> todayRequests = logRepository.findByApiKeyAndTimestampAfter(apiKey, startOfDay);

        if (todayRequests.size() >= requestLimit) {
            throw new RuntimeException("Daily request limit exceeded.");
        }
    }

    @Cacheable(value = "exchangeRates", key = "#from + ':' + #to", unless = "#result == null")
    public Double getCachedExchangeRate(String from, String to) {
        String rateCacheKey = "rate:" + from + ":" + to;
        String cachedRate = redisTemplate.opsForValue().get(rateCacheKey);

        if (cachedRate != null) {
            return Double.parseDouble(cachedRate);
        }

        String url = UriComponentsBuilder.fromHttpUrl(exchangeApiUrl)
                .queryParam("app_id", appId)
                .queryParam("base", from)
                .queryParam("symbols", to)
                .toUriString();

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null || !response.containsKey("rates")) {
            throw new RuntimeException("Failed to fetch exchange rates");
        }

        Map<String, Double> rates = (Map<String, Double>) response.get("rates");
        Double conversionRate = rates.get(to);

        redisTemplate.opsForValue().set(rateCacheKey, String.valueOf(conversionRate), 30, TimeUnit.MINUTES);

        return conversionRate;
    }
}