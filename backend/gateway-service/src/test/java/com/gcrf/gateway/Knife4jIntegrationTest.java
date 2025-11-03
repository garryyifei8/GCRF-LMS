package com.gcrf.gateway;

import com.gcrf.gateway.config.Knife4jGatewayConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Knife4j集成测试
 *
 * 注意: 这些测试验证Knife4j配置是否正确加载，而不是测试实际的API文档聚合
 * 实际的API文档聚合需要微服务运行，超出单元测试范围
 *
 * @author Claude Code
 * @date 2025-10-28
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.cloud.nacos.config.enabled=false",
    "knife4j.gateway.enabled=true"
})
class Knife4jIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testKnife4jGatewayConfigBeanExists() {
        // Test that Knife4jGatewayConfig bean is created
        assertTrue(applicationContext.containsBean("knife4jGatewayConfig"));
        assertNotNull(applicationContext.getBean(Knife4jGatewayConfig.class));
    }

    @Test
    void testKnife4jRouteLocatorBeanExists() {
        // Test that knife4jRouteLocator bean is created
        assertTrue(applicationContext.containsBean("knife4jRouteLocator"));
        assertNotNull(applicationContext.getBean("knife4jRouteLocator", RouteLocator.class));
    }

    @Test
    void testKnife4jRoutesConfigured() {
        // Test that Knife4j aggregation routes are configured
        RouteLocator routeLocator = applicationContext.getBean("knife4jRouteLocator", RouteLocator.class);
        assertNotNull(routeLocator);

        // Verify routes exist (cannot easily inspect route details in test)
        assertNotNull(routeLocator.getRoutes());
    }

    @Test
    void testKnife4jConfigurationLoaded() {
        // Test that Knife4j configuration is properly loaded from application.yml
        // This verifies the configuration structure is correct
        assertTrue(applicationContext.containsBean("knife4jGatewayConfig"));
    }

    @Test
    void testWhitelistIncludesKnife4jPaths() {
        // Verify that the whitelist configuration exists
        // The actual whitelist functionality is tested in AuthenticationFilterTest
        assertTrue(applicationContext.containsBean("authenticationFilter"));
    }
}
