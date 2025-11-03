package com.gcrf.library.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康检查和路由信息Controller
 *
 * @author 王五
 * @date 2025-10-11
 */
@Slf4j
@RestController
@RequestMapping("/actuator")
public class HealthController {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Autowired
    private DiscoveryClient discoveryClient;

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", LocalDateTime.now());
        result.put("service", "library-gateway");
        return result;
    }

    /**
     * 获取所有路由信息
     */
    @GetMapping("/routes")
    public Flux<RouteDefinition> getRoutes() {
        return routeDefinitionLocator.getRouteDefinitions();
    }

    /**
     * 获取所有注册的服务
     */
    @GetMapping("/services")
    public Map<String, Object> getServices() {
        List<String> services = discoveryClient.getServices();

        Map<String, Object> result = new HashMap<>();
        result.put("count", services.size());
        result.put("services", services);
        result.put("timestamp", LocalDateTime.now());

        return result;
    }

    /**
     * 获取网关信息
     */
    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", "library-gateway");
        result.put("version", "1.0.0");
        result.put("description", "国创睿峰智能图书馆管理系统 - API网关");
        result.put("timestamp", LocalDateTime.now());

        return result;
    }
}
