package com.zhs.realtime;

import com.zhs.config.EngineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RealtimeEchoWebSocketHandler extends BinaryWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(RealtimeEchoWebSocketHandler.class);
    private static final int SEQ_BYTES = 4;
    private static final CloseStatus PYTHON_UNAVAILABLE = new CloseStatus(1011, "realtime python unavailable");

    private final EngineProperties engineProperties;
    private final Map<String, RealtimePythonSession> pythonSessions = new ConcurrentHashMap<>();

    public RealtimeEchoWebSocketHandler(EngineProperties engineProperties) {
        this.engineProperties = engineProperties;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            RealtimePythonSession pythonSession = new RealtimePythonSession(session.getId(), engineProperties);
            pythonSessions.put(session.getId(), pythonSession);
            log.info("[realtime-ws] connected session={} pythonPid={}", session.getId(), pythonSession.pid());
        } catch (IOException exception) {
            log.error("[realtime-ws] failed to start python session={}", session.getId(), exception);
            session.close(PYTHON_UNAVAILABLE);
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        RealtimePythonSession pythonSession = pythonSessions.get(session.getId());
        if (pythonSession == null || !pythonSession.isAlive()) {
            closeWithPythonUnavailable(session, "python session missing or not alive");
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
