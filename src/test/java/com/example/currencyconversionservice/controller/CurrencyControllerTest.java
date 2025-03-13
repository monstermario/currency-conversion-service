package com.example.currencyconversionservice.controller;

import com.example.currencyconversionservice.model.RequestLog;
import com.example.currencyconversionservice.model.User;
import com.example.currencyconversionservice.respository.RequestLogRepository;
import com.example.currencyconversionservice.respository.UserRepository;
import com.example.currencyconversionservice.service.ApiKeyService;
import com.example.currencyconversionservice.service.CurrencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrencyControllerTest {

    @Mock
    private CurrencyService currencyService;

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RequestLogRepository requestLogRepository;

    @InjectMocks
    private CurrencyController currencyController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void convertCurrency_SuccessfulConversion() {
        String apiKey = "valid-api-key";
        String from = "USD";
        String to = "EUR";
        double amount = 100.0;
        double convertedAmount = 90.0;

        when(apiKeyService.isValidApiKey(apiKey)).thenReturn(true);
        when(currencyService.isValidCurrency(from)).thenReturn(true);
        when(currencyService.isValidCurrency(to)).thenReturn(true);
        when(currencyService.convertCurrency(apiKey, from, to, amount)).thenReturn(convertedAmount);

        Map<String, Object> response = currencyController.convertCurrency(apiKey, from, to, amount);

        assertEquals(90.0, response.get("convertedAmount"));
        verify(apiKeyService).isValidApiKey(apiKey);
        verify(currencyService).convertCurrency(apiKey, from, to, amount);
    }

    @Test
    void convertCurrency_InvalidApiKey_ShouldThrowUnauthorized() {
        String apiKey = "invalid-api-key";

        when(apiKeyService.isValidApiKey(apiKey)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                currencyController.convertCurrency(apiKey, "USD", "EUR", 100.0)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void convertCurrency_InvalidAmount_ShouldThrowBadRequest() {
        String apiKey = "valid-api-key";

        when(apiKeyService.isValidApiKey(apiKey)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                currencyController.convertCurrency(apiKey, "USD", "EUR", -10.0)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void convertCurrency_InvalidCurrencyCode_ShouldThrowBadRequest() {
        String apiKey = "valid-api-key";
        String from = "INVALID";
        String to = "EUR";

        when(apiKeyService.isValidApiKey(apiKey)).thenReturn(true);
        when(currencyService.isValidCurrency(from)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                currencyController.convertCurrency(apiKey, from, to, 100.0)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void getConversionLogs_ValidApiKey_ShouldReturnLogs() {
        String apiKey = "valid-api-key";
        List<RequestLog> logs = List.of(new RequestLog());

        when(apiKeyService.isValidApiKey(apiKey)).thenReturn(true);
        when(requestLogRepository.findByApiKey(apiKey)).thenReturn(logs);

        List<RequestLog> result = currencyController.getConversionLogs(apiKey);

        assertEquals(logs, result);
        verify(apiKeyService).isValidApiKey(apiKey);
        verify(requestLogRepository).findByApiKey(apiKey);
    }

    @Test
    void getConversionLogs_InvalidApiKey_ShouldThrowUnauthorized() {
        String apiKey = "invalid-api-key";

        when(apiKeyService.isValidApiKey(apiKey)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                currencyController.getConversionLogs(apiKey)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void registerUser_NewUser_ShouldReturnApiKey() {
        String name = "John Doe";
        String generatedApiKey = "new-api-key";

        when(userRepository.findByName(name)).thenReturn(Optional.empty());

        Map<String, String> response = currencyController.registerUser(name);

        assertNotNull(response.get("apiKey"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_ExistingUser_ShouldThrowBadRequest() {
        String name = "John Doe";

        when(userRepository.findByName(name)).thenReturn(Optional.of(new User(1L, "existing-api-key", name)));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                currencyController.registerUser(name)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
