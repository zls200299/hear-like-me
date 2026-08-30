package com.zhs.realtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 开发阶段 WebSocket Echo：收到二进制后原样返回，不解析 PCM。
 */
@Slf4j
@Component
public class RealtimeEchoWebSocketHandler extends BinaryWebSocketHandler {

    private final ConcurrentHashMap<String, AtomicInteger> sessionFrameCounts = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionFrameCounts.put(session.getId(), new AtomicInteger(0));
        log.info("[realtime-echo] connected session={}", session.getId());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException {
        ByteBuffer payload = message.getPayload();
        byte[] bytes = new byte[payload.remaining()];
        payload.get(bytes);

        AtomicInteger counter = sessionFrameCounts.get(session.getId());
        int seq = counter != null ? counter.incrementAndGet() : 0;
        log.info("[realtime-echo] frame session={}, seq={}, bytes={}", session.getId(), seq, bytes.length);

        session.sendMessage(new BinaryMessage(bytes));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        AtomicInteger counter = sessionFrameCounts.remove(session.getId());
        int frames = counter != null ? counter.get() : 0;
        log.info("[realtime-echo] closed session={}, frames={}", session.getId(), frames);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("[realtime-echo] transport error session={}", session.getId(), exception);
        sessionFrameCounts.remove(session.getId());
    }
}
