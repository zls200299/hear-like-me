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

    private final String webSocketSessionId;
    private final Process process;
    private final DataInputStream pythonStdout;
    private final DataOutputStream pythonStdin;
    private volatile boolean closed;

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

    public synchronized byte[] processFrame(int seq, byte[] pcm) throws IOException {
        if (closed || !process.isAlive()) {
            throw new IOException("Python process is not alive");
        }
        if (pcm == null) {
            throw new IOException("PCM payload is null");
        }
        if (pcm.length > MAX_PCM_BYTES) {
            throw new IOException("PCM payload exceeds max size: " + pcm.length);
        }

        long startNanos = System.nanoTime();

        pythonStdin.writeInt(seq);
        pythonStdin.writeInt(pcm.length);
        pythonStdin.write(pcm);
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
