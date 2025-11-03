# Service Integration with Nacos - Quick Start Guide

## Overview
This guide shows how to integrate GCRF services with Nacos for service discovery and configuration management.

---

## 1. Service Configuration

### Basic Setup (application.yml)
```yaml
server:
  port: 8081

spring:
  application:
    name: auth-service  # This name is used for service discovery

  # Nacos Discovery
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
        username: ${NACOS_USERNAME:nacos}
        password: ${NACOS_PASSWORD:nacos}
        namespace: ${NACOS_NAMESPACE:}
        group: ${NACOS_GROUP:DEFAULT_GROUP}

        # Optional metadata for advanced routing
        metadata:
          version: 1.0.0
          region: us-east
          zone: zone-1
```

### Configuration Management (bootstrap.yml)
```yaml
# bootstrap.yml - loaded before application.yml
spring:
  application:
    name: auth-service

  cloud:
    nacos:
      config:
        server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
        username: ${NACOS_USERNAME:nacos}
        password: ${NACOS_PASSWORD:nacos}
        namespace: ${NACOS_NAMESPACE:}
        group: ${NACOS_GROUP:DEFAULT_GROUP}

        # Service-specific config
        file-extension: yml

        # Shared configurations
        shared-configs:
          - data-id: application-shared.yml
            group: DEFAULT_GROUP
            refresh: true
          - data-id: jwt-shared.yml
            group: DEFAULT_GROUP
            refresh: true
          - data-id: redis-shared.yml
            group: DEFAULT_GROUP
            refresh: true
          - data-id: database-shared.yml
            group: DEFAULT_GROUP
            refresh: true
```

---

## 2. Service Discovery in Code

### Using Feign Client
```java
// Enable Feign clients in main application
@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}

// Define Feign client for another service
@FeignClient(name = "reader-service")
public interface ReaderServiceClient {

    @GetMapping("/api/v1/readers/{id}")
    ReaderVO getReaderById(@PathVariable Long id);

    @PostMapping("/api/v1/readers/validate")
    ValidationResult validateReader(@RequestBody ReaderValidationDTO dto);
}

// Use in service
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ReaderServiceClient readerClient;

    public AuthResult authenticate(String username, String password) {
        // Service discovery happens automatically
        ReaderVO reader = readerClient.getReaderById(123L);
        // ... authentication logic
    }
}
```

### Using RestTemplate with LoadBalancer
```java
@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced  // Enable load balancing
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final RestTemplate restTemplate;

    public List<BookVO> searchBooks(String query) {
        // Use service name instead of IP:port
        String url = "http://book-service/api/v1/books/search?q=" + query;

        ResponseEntity<List<BookVO>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<BookVO>>() {}
        );

        return response.getBody();
    }
}
```

---

## 3. Dynamic Configuration Refresh

### Using @RefreshScope
```java
@RestController
@RefreshScope  // Enable dynamic refresh
@RequestMapping("/api/v1/config")
public class ConfigController {

    @Value("${feature.newFeature.enabled:false}")
    private boolean newFeatureEnabled;

    @Value("${rate.limit:100}")
    private int rateLimit;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    @GetMapping("/feature-status")
    public Map<String, Object> getFeatureStatus() {
        return Map.of(
            "newFeatureEnabled", newFeatureEnabled,
            "rateLimit", rateLimit,
            "jwtExpiration", jwtExpiration
        );
    }
}
```

### Configuration Listener
```java
@Component
@Slf4j
public class NacosConfigListener {

    @NacosConfigListener(
        dataId = "application-shared.yml",
        group = "DEFAULT_GROUP",
        autoRefreshed = true
    )
    public void onSharedConfigChange(String config) {
        log.info("Shared configuration updated");
        // Handle configuration change
        // e.g., clear caches, reload settings, etc.
    }

    @NacosConfigListener(
        dataId = "jwt-shared.yml",
        group = "DEFAULT_GROUP",
        autoRefreshed = true
    )
    public void onJwtConfigChange(String config) {
        log.info("JWT configuration updated, refreshing security settings");
        // Update JWT validator settings
    }
}
```

---

## 4. Gateway Integration

### Dynamic Routing with Service Discovery
```yaml
# gateway-service application.yml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true

      routes:
        # Route to auth service
        - id: auth-service
          uri: lb://auth-service  # Load balanced URI
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20

        # Route to book service
        - id: book-service
          uri: lb://book-service
          predicates:
            - Path=/api/v1/books/**
          filters:
            - name: CircuitBreaker
              args:
                name: bookServiceCircuitBreaker
                fallbackUri: forward:/fallback/books
```

---

## 5. Health Check Implementation

### Custom Health Indicator
```java
@Component
public class NacosHealthIndicator implements HealthIndicator {

    @Autowired
    private NacosDiscoveryProperties nacosProperties;

    @Autowired
    private NamingService namingService;

    @Override
    public Health health() {
        try {
            // Check Nacos connection
            String serverStatus = namingService.getServerStatus();

            // Get service instance count
            List<Instance> instances = namingService.getAllInstances(
                nacosProperties.getService()
            );

            return Health.up()
                .withDetail("server", nacosProperties.getServerAddr())
                .withDetail("status", serverStatus)
                .withDetail("instances", instances.size())
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

---

## 6. Testing Service Discovery

### Integration Test
```java
@SpringBootTest
@AutoConfigureMockMvc
public class ServiceDiscoveryIntegrationTest {

    @Autowired
    private DiscoveryClient discoveryClient;

    @Test
    public void testServiceRegistration() {
        // Check if service is registered
        List<String> services = discoveryClient.getServices();
        assertTrue(services.contains("auth-service"));

        // Get service instances
        List<ServiceInstance> instances = discoveryClient
            .getInstances("auth-service");

        assertFalse(instances.isEmpty());

        ServiceInstance instance = instances.get(0);
        assertNotNull(instance.getHost());
        assertTrue(instance.getPort() > 0);
    }

    @MockBean
    private ReaderServiceClient readerClient;

    @Test
    public void testFeignClientCall() {
        // Mock Feign client response
        ReaderVO mockReader = new ReaderVO();
        mockReader.setId(1L);
        mockReader.setName("Test Reader");

        when(readerClient.getReaderById(1L))
            .thenReturn(mockReader);

        // Test service call
        ReaderVO reader = readerClient.getReaderById(1L);

        assertNotNull(reader);
        assertEquals("Test Reader", reader.getName());
    }
}
```

---

## 7. Docker Compose Integration

### Service Environment Variables
```yaml
# docker-compose.yml
services:
  auth-service:
    image: gcrf-library/auth-service:latest
    environment:
      - SERVER_PORT=8081
      - SPRING_PROFILES_ACTIVE=prod
      - NACOS_SERVER_ADDR=nacos:8848
      - NACOS_USERNAME=nacos
      - NACOS_PASSWORD=${NACOS_PASSWORD}
      - NACOS_NAMESPACE=prod
      - NACOS_GROUP=LIBRARY_GROUP
    depends_on:
      - nacos
      - postgres
      - redis
    networks:
      - gcrf-network
```

---

## 8. Deployment Checklist

### Pre-deployment
- [ ] Nacos server is running and accessible
- [ ] Shared configurations are pushed to Nacos
- [ ] Database schemas are created
- [ ] Redis is running
- [ ] RabbitMQ is running (if using messaging)

### Service Startup Order
1. Infrastructure (Nacos, DB, Redis, RabbitMQ)
2. Push configurations to Nacos
3. Start Gateway Service
4. Start Auth Service
5. Start other services (Book, Reader, Circulation, etc.)

### Verification
```bash
# 1. Check Nacos is accessible
curl http://localhost:8848/nacos/

# 2. Test service discovery
./deployment/scripts/test-service-discovery.sh

# 3. Verify configurations
./deployment/scripts/push-nacos-configs.sh verify

# 4. Check service health
curl http://localhost:8080/actuator/health

# 5. Monitor in Nacos dashboard
open http://localhost:8848/nacos/
```

---

## 9. Troubleshooting Quick Guide

### Service not visible in Nacos
```bash
# Check service logs
docker logs auth-service

# Common issues:
# 1. Wrong NACOS_SERVER_ADDR
# 2. Authentication failure
# 3. Network connectivity
# 4. Wrong namespace/group
```

### Configuration not loading
```bash
# Verify config exists
curl "http://localhost:8848/nacos/v1/cs/configs?dataId=jwt-shared.yml&group=DEFAULT_GROUP"

# Check bootstrap.yml exists and is correct
# Ensure spring-cloud-starter-alibaba-nacos-config dependency is included
```

### Service discovery not working
```bash
# Check if services are registered
curl "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=auth-service"

# Ensure @EnableDiscoveryClient annotation is present
# Verify Feign/RestTemplate has @LoadBalanced
```

---

## Related Documentation
- [Nacos Configuration Guide](./NACOS_CONFIGURATION.md)
- [Service Discovery Scripts](../scripts/SERVICE_DISCOVERY_README.md)
- [Infrastructure Setup](../INFRASTRUCTURE_README.md)