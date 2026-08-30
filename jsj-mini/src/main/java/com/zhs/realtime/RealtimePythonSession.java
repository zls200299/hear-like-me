package com.zhs.realtime;

import com.zhs.config.EngineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class RealtimePythonSession implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(RealtimePythonSession.class);
    private static final int MAX_PCM_BYTES = 1024 * 1024;
    private static final int DESTROY_WAIT_MS = 500;
    private static final int WARMUP_SEQ = 0;

    private final String webSocketSessionId;
    private final Process process;
    private final DataInputStream pythonStdout;
    private final DataOutputStream pythonStdin;
    private volatile boolean closed;
    private volatile boolean ready;

    public RealtimePythonSession(String webSocketSessionId, EngineProperties engineProperties) throws IOException {
        this.webSocketSessionId = webSocketSessionId;

        ProcessBuilder processBuilder = new ProcessBuilder(
                engineProperties.getPythonPath(),
                engineProperties.resolveRealtimeScriptPath().toString()
        );
        processBuilder.redirectErrorStream(false);
        this.process = processBuilder.start();

        this.pythonStdout = new DataInputStream(process.getInputStream());
        this.pythonStdin = new DataOutputStream(process.getOutputStream());

        Thread stderrConsumer = new Thread(
                () -> consumeStderr(process.getErrorStream()),
                "realtime-python-stderr-" + webSocketSessionId
        );
        stderrConsumer.setDaemon(true);
        stderrConsumer.start();

        log.info("[realtime-python] started session={} pid={}", webSocketSessionId, process.pid());
        try {
            warmup();
            this.ready = true;
        } catch (IOException exception) {
            close();
            throw exception;
        }
    }

    private void consumeStderr(InputStream stderr) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stderr, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[realtime-python] stderr session={} {}", webSocketSessionId, line);
            }
        } catch (IOException exception) {
            if (!closed) {
                log.debug("[realtime-python] stderr closed session={}", webSocketSessionId, exception);
            }
        }
    }

    private void warmup() throws IOException {
        long startNanos = System.nanoTime();
        byte[] returnedPcm = exchangeFrame(WARMUP_SEQ, new byte[0]);
        if (returnedPcm.length != 0) {
            throw new IOException("Warmup returned unexpected PCM length: " + returnedPcm.length);
        }
        long warmupMs = (System.nanoTime() - startNanos) / 1_000_000L;
        log.info(
                "[realtime-python] ready session={} pid={} warmupMs={}",
                webSocketSessionId,
                process.pid(),
                warmupMs
        );
    }

    public synchronized byte[] processFrame(int seq, byte[] pcm) throws IOException {
        if (!ready) {
            throw new IOException("Python session is not ready");
        }
        if (pcm == null) {
            throw new IOException("PCM payload is null");
        }
        if (pcm.length > MAX_PCM_BYTES) {
            throw new IOException("PCM payload exceeds max size: " + pcm.length);
        }

        long startNanos = System.nanoTime();
        byte[] returnedPcm = exchangeFrame(seq, pcm);
        long pythonMs = (System.nanoTime() - startNanos) / 1_000_000L;
        log.info(
                "[realtime-python] session={} seq={} pcmBytes={} pythonMs={}",
                webSocketSessionId,
                seq,
                pcm.length,
                pythonMs
        );
        return returnedPcm;
    }

    private byte[] exchangeFrame(int seq, byte[] pcm) throws IOException {
        if (closed || !process.isAlive()) {
            throw new IOException("Python process is not alive");
        }

        pythonStdin.writeInt(seq);
        pythonStdin.writeInt(pcm.length);
        if (pcm.length > 0) {
            pythonStdin.write(pcm);
        }
        pythonStdin.flush();

        int returnedSeq = pythonStdout.readInt();
        int returnedLength = pythonStdout.readInt();

        if (returnedSeq != seq) {
            throw new IOException("Seq mismatch: expected " + seq + ", got " + returnedSeq);
        }
        if (returnedLength < 0 || returnedLength > MAX_PCM_BYTES) {
            throw new IOException("Invalid returned PCM length: " + returnedLength);
        }

        byte[] returnedPcm = new byte[returnedLength];
        if (returnedLength > 0) {
            pythonStdout.readFully(returnedPcm);
        }
        return returnedPcm;
    }

    public boolean isReady() {
        return ready && isAlive();
    }

    public boolean isAlive() {
        return !closed && process.isAlive();
    }

    public long pid() {
        return process.pid();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        ready = false;

        try {
            pythonStdin.close();
        } catch (IOException exception) {
            log.debug("[realtime-python] stdin close failed session={}", webSocketSessionId, exception);
        }

        try {
            pythonStdout.close();
        } catch (IOException exception) {
            log.debug("[realtime-python] stdout close failed session={}", webSocketSessionId, exception);
        }

        process.destroy();
        try {
            if (!process.waitFor(DESTROY_WAIT_MS, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }

        log.info("[realtime-python] closed session={} pid={}", webSocketSessionId, process.pid());
    }
}
