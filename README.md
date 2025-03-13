# 🚀 Currency Conversion Service

## Overview
The **Currency Conversion Service** is a RESTful API that allows users to convert currencies in using exchange rates from **Open Exchange Rates API**. The service implements **API key-based authentication**, **rate-limiting**, and **caching** to optimize performance.

---

## 📌 Features
✅ **Currency Conversion** – Convert an amount from one currency to another using exchange rates.  
✅ **User Registration** – Generate an API key to access the conversion service.  
✅ **API Key Authentication** – Secure endpoints using API keys.  
✅ **Request Logging** – Keep track of conversion requests per user.  
✅ **Rate Limiting** – Prevent excessive API usage using request logs.  
✅ **Caching** – Reduce API calls using **Redis** caching for exchange rates.  
✅ **Security** – Implements **Spring Security** with CSRF disabled for APIs.

---

## 🛠️ Tech Stack
| **Technology**  | **Purpose** |
|----------------|------------|
| **Spring Boot** | Backend framework |
| **Spring Security** | API security |
| **Spring Data JPA** | ORM for database interactions |
| **PostgreSQL** | Relational database for storing users and logs |
| **Redis** | Caching exchange rates and API responses |
| **JUnit & Mockito** | Unit testing |

---

## 📂 Project Structure
```
currency-conversion-service/
│── src/main/java/com/example/currencyconversionservice
│   ├── config/                   # Security configuration
│   ├── controller/                # REST API controllers
│   ├── exception/                 # Global exception handling
│   ├── model/                     # JPA entities
│   ├── repository/                # Spring Data JPA repositories
│   ├── service/                   # Business logic & currency conversion
│   ├── CurrencyConversionServiceApplication.java  # Main application entry point
│── src/test/java/com/example/currencyconversionservice  # Unit tests
│── src/main/resources/
│   ├── application.properties      # Environment configurations
│── README.md
│── .gitignore
│── pom.xml
```

---

## 🚀 Getting Started
### 1️⃣ Prerequisites
Ensure you have the following installed:
- Java 17+
- PostgreSQL
- Redis
- Maven

### 2️⃣ Clone the Repository
```
git clone https://github.com/monstermario/currency-conversion-service.git
cd currency-conversion-service
```

### 3️⃣ Configure the Database
Update `application.properties` with your PostgreSQL credentials:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/currencydb
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### 4️⃣ Run Redis
Ensure Redis is running on **localhost:6379**:
```
redis-server
```

### 5️⃣ Build & Run the Project
```
mvn clean install
mvn spring-boot:run
```

### 6️⃣ Test API with cURL
#### 🔹 Register a User & Get API Key
```
curl -X POST "http://localhost:8080/api/register?name=JohnDoe"
```
**Response:**
```json
{ "apiKey": "generated-api-key" }
```

#### 🔹 Convert Currency
```
curl -H "X-API-KEY: api-key" \
     "http://localhost:8080/api/convert?from=USD&to=EUR&amount=100"
```
**Response:**
```json
{ "convertedAmount": 92.5 }
```

#### 🔹 View Conversion Logs
```
curl -H "X-API-KEY: api-key" \
     "http://localhost:8080/api/logs"
```

---

## 🔐 Security & Rate Limiting
- **API Key Required:** Every request requires an `X-API-KEY` header.
- **Request Rate Limits:**
    - **Max 1 request per 2 minutes**.
    - **Max 100 requests per day (200 on weekends).**
- **Unauthorized requests return** `401 Unauthorized`.

## 🧪 Testing
Run **unit tests** using:

```
mvn test
```


