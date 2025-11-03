package com.gcrf.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gateway启动测试
 *
 * @author Claude Code
 * @date 2025-10-28
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.cloud.nacos.config.enabled=false"
})
class GatewayApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        // Test that Spring context loads successfully
        assertNotNull(applicationContext);
    }

    @Test
    void gatewayApplicationBeanExists() {
        // Test that GatewayApplication bean is created
        assertNotNull(applicationContext.getBean(GatewayApplication.class));
    }

    @Test
    void authenticationFilterBeanExists() {
        // Test that AuthenticationFilter is loaded
        assertTrue(applicationContext.containsBean("authenticationFilter"));
    }

    @Test
    void logFilterBeanExists() {
        // Test that LogFilter is loaded
        assertTrue(applicationContext.containsBean("logFilter"));
    }

    @Test
    void gatewaySecurityConfigBeanExists() {
        // Test that GatewaySecurityConfig is loaded
        assertTrue(applicationContext.containsBean("gatewaySecurityConfig"));
    }
}
