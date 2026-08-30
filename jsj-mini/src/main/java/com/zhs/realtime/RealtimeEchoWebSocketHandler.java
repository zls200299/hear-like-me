package com.zhs.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhs.config.EngineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class RealtimeEchoWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(RealtimeEchoWebSocketHandler.class);
    private static final int SEQ_BYTES = 4;
    private static final String READY_MESSAGE = "{\"type\":\"READY\"}";
    private static final String PARAM_ERROR_NOT_READY = "{\"type\":\"PARAM_ERROR\",\"version\":1,\"message\":\"python not ready\"}";
    private static final CloseStatus PYTHON_UNAVAILABLE = new CloseStatus(1011, "realtime python unavailable");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final EngineProperties engineProperties;
    private final Map<String, RealtimePythonSession> pythonSessions = new ConcurrentHashMap<>();
    private final ExecutorService pythonBootstrapExecutor = Executors.newCachedThreadPool((runnable) -> {
        Thread thread = new Thread(runnable, "realtime-python-bootstrap");
        thread.setDaemon(true);
        return thread;
    });

    public RealtimeEchoWebSocketHandler(EngineProperties engineProperties) {
        this.engineProperties = engineProperties;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = session.getId();
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return new RealtimePythonSession(sessionId, engineProperties);
                    } catch (IOException exception) {
                        throw new CompletionException(exception);
                    }
                }, pythonBootstrapExecutor)
                .whenComplete((pythonSession, error) -> {
                    if (error != null) {
                        Throwable cause = error.getCause() != null ? error.getCause() : error;
                        log.error("[realtime-ws] failed to warm up python session={}", sessionId, cause);
                        try {
                            if (session.isOpen()) {
                                session.close(PYTHON_UNAVAILABLE);
                            }
                        } catch (IOException closeException) {
                            log.debug("[realtime-ws] close after warmup failure failed session={}", sessionId, closeException);
                        }
                        return;
                    }

                    if (!session.isOpen()) {
                        pythonSession.close();
                        log.info("[realtime-ws] socket closed before ready session={} pid={}", sessionId, pythonSession.pid());
                        return;
                    }

                    pythonSessions.put(sessionId, pythonSession);
                    try {
                        session.sendMessage(new TextMessage(READY_MESSAGE));
                        log.info(
                                "[realtime-ws] ready session={} pythonPid={}",
                                sessionId,
                                pythonSession.pid()
                        );
                    } catch (IOException exception) {
                        log.error("[realtime-ws] failed to send READY session={}", sessionId, exception);
                        closeWithPythonUnavailable(session, exception.getMessage());
                    }
                });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        RealtimePythonSession pythonSession = pythonSessions.get(session.getId());
        if (pythonSession == null || !pythonSession.isReady()) {
            session.sendMessage(new TextMessage(PARAM_ERROR_NOT_READY));
            return;
        }

        String payload = message.getPayload();
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (!"PARAM_UPDATE".equals(root.path("type").asText())) {
                log.warn("[realtime-ws] ignore unsupported text message session={}", session.getId());
                return;
            }

            String response = pythonSession.updateParams(payload);
            if (!session.isOpen()) {
                return;
            }
            session.sendMessage(new TextMessage(response));
            log.info("[realtime-ws] param update session={} response={}", session.getId(), response);
        } catch (IOException exception) {
            log.error("[realtime-ws] param update failed session={}", session.getId(), exception);
            String errorResponse = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "type", "PARAM_ERROR",
                            "version", 1,
                            "message", exception.getMessage() == null ? "param update failed" : exception.getMessage()
                    )
            );
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(errorResponse));
            }
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        RealtimePythonSession pythonSession = pythonSessions.get(session.getId());
        if (pythonSession == null || !pythonSession.isReady()) {
            closeWithPythonUnavailable(session, "python session missing or not ready");
            return;
        }

        ByteBuffer payload = message.getPayload();
        if (payload.remaining() < SEQ_BYTES) {
            log.warn("[realtime-ws] binary too short session={} bytes={}", session.getId(), payload.remaining());
            return;
        }

        int seq = payload.getInt();
        byte[] pcm = new byte[payload.remaining()];
        payload.get(pcm);

        try {
            byte[] returnedPcm = pythonSession.processFrame(seq, pcm);
            ByteBuffer response = ByteBuffer.allocate(SEQ_BYTES + returnedPcm.length).order(ByteOrder.BIG_ENDIAN);
            response.putInt(seq);
            response.put(returnedPcm);

            if (!session.isOpen()) {
                return;
            }
            session.sendMessage(new BinaryMessage(response.array()));
        } catch (IOException exception) {
            log.error(
                    "[realtime-ws] python frame failed session={} seq={} pcmBytes={}",
                    session.getId(),
                    seq,
                    pcm.length,
                    exception
            );
            closeWithPythonUnavailable(session, exception.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cleanupPythonSession(session.getId());
        log.info("[realtime-ws] closed session={} status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[realtime-ws] transport error session={}", session.getId(), exception);
        cleanupPythonSession(session.getId());
        try {
            if (session.isOpen()) {
                session.close(PYTHON_UNAVAILABLE);
            }
        } catch (IOException closeException) {
            log.debug("[realtime-ws] close after transport error failed session={}", session.getId(), closeException);
        }
    }

    private void closeWithPythonUnavailable(WebSocketSession session, String reason) {
        cleanupPythonSession(session.getId());
        try {
            if (session.isOpen()) {
                session.close(PYTHON_UNAVAILABLE);
            }
        } catch (IOException exception) {
            log.debug("[realtime-ws] close failed session={} reason={}", session.getId(), reason, exception);
        }
    }

    private void cleanupPythonSession(String sessionId) {
        RealtimePythonSession pythonSession = pythonSessions.remove(sessionId);
        if (pythonSession != null) {
            pythonSession.close();
        }
    }
}
