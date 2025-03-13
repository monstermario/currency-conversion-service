package com.example.currencyconversionservice.respository;

import com.example.currencyconversionservice.model.RequestLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {
    List<RequestLog> findByApiKey(String apiKey);
    List<RequestLog> findByApiKeyAndTimestampAfter(String apiKey, LocalDateTime timestamp);
}