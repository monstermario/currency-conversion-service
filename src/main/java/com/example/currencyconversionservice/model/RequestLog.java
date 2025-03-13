package com.example.currencyconversionservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "request_logs")
public class RequestLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String apiKey;
    private String fromCurrency;
    private String toCurrency;
    private Double amount;
    private Double convertedAmount;
    private LocalDateTime timestamp;
}