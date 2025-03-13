package com.example.currencyconversionservice.service;

import com.example.currencyconversionservice.model.RequestLog;
import com.example.currencyconversionservice.model.User;
import com.example.currencyconversionservice.respository.RequestLogRepository;
import com.example.currencyconversionservice.respository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrencyServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RequestLogRepository logRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private CurrencyService currencyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        currencyService.exchangeApiUrl = "https://api.openexchangerates.org/latest.json";
        currencyService.appId = "test-app-id";
    }

    @Test
    void convertCurrency_ValidRequest_ShouldReturnConvertedAmount() {
        String apiKey = "valid-api-key";
        String from = "USD";
        String to = "EUR";
        double amount = 100.0;
        double conversionRate = 0.9;
        double expectedConvertedAmount = amount * conversionRate;

        User mockUser = new User(1L, apiKey, "TestUser");
        when(userRepository.findByApiKey(apiKey)).thenReturn(Optional.of(mockUser));

        when(valueOperations.get("conversion:USD:EUR:100.00")).thenReturn(null);
        when(valueOperations.get("rate:USD:EUR")).thenReturn(String.valueOf(conversionRate));

        double result = currencyService.convertCurrency(apiKey, from, to, amount);

        assertEquals(expectedConvertedAmount, result);
        verify(logRepository).save(any(RequestLog.class));
    }

    @Test
    void convertCurrency_InvalidApiKey_ShouldThrowException() {
        String apiKey = "invalid-api-key";

        when(userRepository.findByApiKey(apiKey)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            currencyService.convertCurrency(apiKey, "USD", "EUR", 100.0);
        });

        assertEquals("Invalid API Key", exception.getMessage());
    }

    @Test
    void convertCurrency_RequestLimitExceeded_ShouldThrowException() {
        String apiKey = "valid-api-key";
        User mockUser = new User(1L, apiKey, "TestUser");
        when(userRepository.findByApiKey(apiKey)).thenReturn(Optional.of(mockUser));

        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(2);
        when(logRepository.findByApiKeyAndTimestampAfter(eq(apiKey), any(LocalDateTime.class)))
                .thenReturn(List.of(new RequestLog()));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            currencyService.convertCurrency(apiKey, "USD", "EUR", 100.0);
        });

        assertEquals("You must wait at least 2 minutes before making another request.", exception.getMessage());
    }

    @Test
    void isValidCurrency_ValidCurrency_ShouldReturnTrue() {
        when(setOperations.members("validCurrencies")).thenReturn(Set.of("USD", "EUR", "GBP"));

        assertTrue(currencyService.isValidCurrency("USD"));
    }

    @Test
    void isValidCurrency_InvalidCurrency_ShouldReturnFalse() {
        when(setOperations.members("validCurrencies")).thenReturn(Set.of("USD", "EUR", "GBP"));

        assertFalse(currencyService.isValidCurrency("XYZ"));
    }

    @Test
    void getCachedExchangeRate_CachedRateExists_ShouldReturnCachedRate() {
        when(valueOperations.get("rate:USD:EUR")).thenReturn("0.85");

        Double rate = currencyService.getCachedExchangeRate("USD", "EUR");

        assertEquals(0.85, rate);
    }

    @Test
    void getCachedExchangeRate_CacheMiss_ShouldFetchAndCacheRate() {
        String url = UriComponentsBuilder.fromHttpUrl(currencyService.exchangeApiUrl)
                .queryParam("app_id", currencyService.appId)
                .queryParam("base", "USD")
                .queryParam("symbols", "EUR")
                .toUriString();

        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("rates", Map.of("EUR", 0.88));

        when(valueOperations.get("rate:USD:EUR")).thenReturn(null);
        when(restTemplate.getForObject(url, Map.class)).thenReturn(mockResponse);

        Double rate = currencyService.getCachedExchangeRate("USD", "EUR");

        assertEquals(0.88, rate);
        verify(valueOperations).set(eq("rate:USD:EUR"), eq("0.88"), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    void getCachedExchangeRate_FailedApiCall_ShouldThrowException() {
        String url = UriComponentsBuilder.fromHttpUrl(currencyService.exchangeApiUrl)
                .queryParam("app_id", currencyService.appId)
                .queryParam("base", "USD")
                .queryParam("symbols", "EUR")
                .toUriString();

        when(valueOperations.get("rate:USD:EUR")).thenReturn(null);
        when(restTemplate.getForObject(url, Map.class)).thenReturn(null);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            currencyService.getCachedExchangeRate("USD", "EUR");
        });

        assertEquals("Failed to fetch exchange rates", exception.getMessage());
    }
}
