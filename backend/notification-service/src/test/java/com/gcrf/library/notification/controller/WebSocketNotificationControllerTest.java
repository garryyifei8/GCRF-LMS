package com.gcrf.library.notification.controller;

import com.gcrf.library.common.test.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebSocketNotificationController集成测试
 *
 * 测试覆盖范围：
 * - REST端点: /api/v1/ws/stats, /api/v1/ws/online/{userId}
 * - STOMP端点: /app/ping -> /topic/pong, /app/notifications订阅, /app/push广播
 *
 * 使用RANDOM_PORT以支持真实的WebSocket/STOMP交互，
 * 通过TestRestTemplate测试REST，通过WebSocketStompClient测试STOMP。
 *
 * @author GCRF Team
 * @since 2026-04-13
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class WebSocketNotificationControllerTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private WebSocketStompClient stompClient;
    private StompSession session;

    @BeforeEach
    void setupStompClient() throws Exception {
        List<Transport> transports = List.of(
            new WebSocketTransport(new StandardWebSocketClient())
        );
        SockJsClient sockJsClient = new SockJsClient(transports);
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        session = stompClient.connectAsync(
            "ws://localhost:" + port + "/ws/notifications",
            new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);
    }

    @AfterEach
    void disconnectStomp() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    // ===== REST tests =====

    @Test
    @SuppressWarnings("rawtypes")
    void getWebSocketStats_shouldReturnOnlineUsers() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/ws/stats", Map.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("onlineUsers");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void checkUserOnline_whenOffline_shouldReturnFalse() {
        Long testUserId = 999999L;
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/ws/online/" + testUserId, Map.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("online")).isEqualTo(false);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void checkUserOnline_shouldReturnStatusField() {
        // Verifies response structure — actual online status depends on session state
        Long testUserId = 123L;
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/ws/online/" + testUserId, Map.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("online");
        assertThat(response.getBody()).containsKey("userId");
    }

    // ===== STOMP tests =====

    @Test
    void stompClient_shouldConnectSuccessfully() {
        // Verify basic connection established in @BeforeEach
        assertThat(session).isNotNull();
        assertThat(session.isConnected()).isTrue();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void handlePing_shouldReceivePongResponse() throws Exception {
        CompletableFuture<Map> pongFuture = new CompletableFuture<>();

        session.subscribe("/topic/pong", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                pongFuture.complete((Map) payload);
            }
        });

        // Allow subscription to propagate through broker
        Thread.sleep(500);

        Map<String, Object> pingPayload = new HashMap<>();
        pingPayload.put("clientTime", System.currentTimeMillis());
        session.send("/app/ping", pingPayload);

        Map pong = pongFuture.get(5, TimeUnit.SECONDS);
        assertThat(pong).isNotNull();
        // Controller returns: { "message": "pong", "timestamp": ..., "received": ... }
        assertThat(pong.get("message")).isEqualTo("pong");
        assertThat(pong).containsKey("timestamp");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void handleSubscribe_shouldReceiveSubscriptionConfirmation() throws Exception {
        CompletableFuture<Map> confirmFuture = new CompletableFuture<>();

        // @SubscribeMapping("/notifications") responds when client subscribes to /app/notifications
        session.subscribe("/app/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                confirmFuture.complete((Map) payload);
            }
        });

        Map confirm = confirmFuture.get(5, TimeUnit.SECONDS);
        assertThat(confirm).isNotNull();
        // Controller returns: { "message": "订阅成功", "userId": ..., "timestamp": ..., "onlineUsers": ... }
        assertThat(confirm).containsKey("message");
        assertThat(confirm).containsKey("userId");
        assertThat(confirm).containsKey("onlineUsers");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void handlePushToTopic_shouldBroadcastToTopic() throws Exception {
        CompletableFuture<Map> received = new CompletableFuture<>();

        session.subscribe("/topic/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.complete((Map) payload);
            }
        });

        // Allow subscription to propagate
        Thread.sleep(500);

        // Match NotificationPushRequest DTO fields:
        // title, content, notificationType, priority, targetType
        Map<String, Object> pushRequest = new HashMap<>();
        pushRequest.put("targetType", "ALL");
        pushRequest.put("title", "Test broadcast");
        pushRequest.put("content", "Hello all");
        pushRequest.put("notificationType", "SYSTEM");
        pushRequest.put("priority", "MEDIUM");
        session.send("/app/push", pushRequest);

        // pushAsync is @Async — give it more time
        Map msg = received.get(8, TimeUnit.SECONDS);
        assertThat(msg).isNotNull();
        assertThat(msg).containsKey("title");
    }
}
