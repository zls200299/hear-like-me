#!/usr/bin/env python3
"""Realtime streaming cochlear vocoder (stdin/stdout binary framing)."""

from __future__ import annotations

import math
import struct
import sys
import time

import numpy as np
from scipy.signal import lfilter

from cochlear_vocoder import biquad_coeffs, clamp, log_edges

MAX_PCM_LENGTH = 1024 * 1024
HEADER_FORMAT = ">II"
HEADER_SIZE = struct.calcsize(HEADER_FORMAT)

SAMPLE_RATE = 44100
N_CHANNELS = 8
F_LO = 150.0
F_HI = 7000.0
CARRIER = "noise"
ENV_CUT = 160.0
SPREAD = 0.15
NOISE_LEVEL = 0.0
ENV_AMP = 2.6
WET = 0.9
COMPRESS_THRESHOLD_DB = -16.0
COMPRESS_RATIO = 4.0
COMPRESS_KNEE_DB = 30.0
COMPRESS_ATTACK = 0.003
COMPRESS_RELEASE = 0.25


class StatefulBiquad:
    def __init__(self, kind: str, f0: float, q: float, sr: int):
        self.b, self.a = biquad_coeffs(kind, f0, q, sr)
        self.zi = np.zeros(2, dtype=np.float64)

    def process(self, x: np.ndarray) -> np.ndarray:
        y, self.zi = lfilter(self.b, self.a, x, zi=self.zi)
        return y


class ChannelState:
    def __init__(self, fc: float, q: float, env_cut: float, sr: int, carrier: str):
        self.fc = fc
        self.q = q
        self.output_q = min(q, 10.0)
        self.analysis_bp = StatefulBiquad("bandpass", fc, q, sr)
        self.envelope_lp = StatefulBiquad("lowpass", env_cut, 0.5, sr)
        self.carrier_bp = (
            StatefulBiquad("bandpass", fc, q, sr) if carrier == "noise" else None
        )
        self.output_bp = StatefulBiquad("bandpass", fc, self.output_q, sr)
        self.sine_phase = 0.0

    def carrier_signal(self, noise: np.ndarray | None, n: int, sr: int) -> np.ndarray:
        if self.carrier_bp is not None:
            return self.carrier_bp.process(noise)
        t = (np.arange(n, dtype=np.float64) + self.sine_phase) / sr
        self.sine_phase += n
        return np.sin(2.0 * math.pi * self.fc * t)


class StreamingVocoder:
    def __init__(self, seed: int | None = None):
        self.sample_rate = SAMPLE_RATE
        self.n_channels = N_CHANNELS
        self.spread = SPREAD
        self.noise_level = NOISE_LEVEL
        self.env_amp = ENV_AMP
        self.wet = WET
        self.compressor_gain_db = 0.0
        self.frame_count = 0
        self.warmed_up = False
        self.rng = np.random.default_rng(seed)

        edges = log_edges(N_CHANNELS, F_LO, F_HI)
        self.channels: list[ChannelState] = []
        for i in range(N_CHANNELS):
            lo, hi = float(edges[i]), float(edges[i + 1])
            fc = math.sqrt(lo * hi)
            bw = max(hi - lo, 1.0)
            q = clamp(fc / bw, 0.5, 18.0)
            self.channels.append(ChannelState(fc, q, ENV_CUT, SAMPLE_RATE, CARRIER))

    def _apply_spread(self, envs: np.ndarray) -> np.ndarray:
        gains = envs.copy()
        if self.spread <= 0:
            return gains

        s = self.spread
        n_channels = envs.shape[0]
        for i in range(n_channels):
            for j, amt in (
                (i - 1, s * 0.60),
                (i + 1, s * 0.60),
                (i - 2, s * 0.28),
                (i + 2, s * 0.28),
            ):
                if 0 <= j < n_channels and amt > 0:
                    gains[j] += envs[i] * amt
        return gains

    def _compress(self, x: np.ndarray) -> np.ndarray:
        eps = 1e-9
        sr = self.sample_rate
        level_db = 20.0 * np.log10(np.abs(x) + eps)
        over = level_db - COMPRESS_THRESHOLD_DB
        gain_db = np.zeros_like(level_db)

        in_knee = (over > -COMPRESS_KNEE_DB / 2.0) & (over <= COMPRESS_KNEE_DB / 2.0)
        above = over > COMPRESS_KNEE_DB / 2.0
        knee = over[in_knee] + COMPRESS_KNEE_DB / 2.0
        gain_db[in_knee] = -(1.0 - 1.0 / COMPRESS_RATIO) * (knee * knee) / (2.0 * COMPRESS_KNEE_DB)
        gain_db[above] = -(1.0 - 1.0 / COMPRESS_RATIO) * over[above]

        a_att = math.exp(-1.0 / (COMPRESS_ATTACK * sr))
        a_rel = math.exp(-1.0 / (COMPRESS_RELEASE * sr))
        smoothed = np.empty_like(gain_db)
        g = self.compressor_gain_db
        for i in range(gain_db.size):
            target = gain_db[i]
            coef = a_att if target < g else a_rel
            g = coef * g + (1.0 - coef) * target
            smoothed[i] = g
        self.compressor_gain_db = float(g)

        return x * (10.0 ** (smoothed / 20.0))

    @staticmethod
    def _float_to_pcm16_le(y: np.ndarray) -> bytes:
        clipped = np.clip(np.asarray(y, dtype=np.float64), -1.0, 1.0)
        pcm = np.where(clipped < 0, clipped * 0x8000, clipped * 0x7FFF).astype("<i2")
        return pcm.tobytes()

    def process_pcm(self, pcm_bytes: bytes) -> tuple[bytes, float, float, float]:
        if len(pcm_bytes) % 2 != 0:
            raise ValueError(f"PCM byte length must be even, got {len(pcm_bytes)}")

        if len(pcm_bytes) == 0:
            return b"", 0.0, 0.0, 0.0

        started = time.perf_counter()
        x = np.frombuffer(pcm_bytes, dtype="<i2").astype(np.float64) / 32768.0
        n = x.size
        in_rms = float(np.sqrt(np.mean(x * x)))

        sig = x
        if self.noise_level > 0:
            # Reserved for later scenario presets; fixed to 0 for B1.
            sig = x

        noise = self.rng.uniform(-1.0, 1.0, n) if CARRIER == "noise" else None

        envs = np.zeros((self.n_channels, n), dtype=np.float64)
        carriers = np.zeros((self.n_channels, n), dtype=np.float64)
        for i, channel in enumerate(self.channels):
            band = channel.analysis_bp.process(sig)
            rect = np.abs(band)
            env = channel.envelope_lp.process(rect)
            envs[i] = env * self.env_amp
            carriers[i] = channel.carrier_signal(noise, n, self.sample_rate)

        gains = self._apply_spread(envs)

        wet = np.zeros(n, dtype=np.float64)
        for i, channel in enumerate(self.channels):
            modulated = carriers[i] * gains[i]
            wet += channel.output_bp.process(modulated)

        y = wet * self.wet
        y = self._compress(y)

        out_rms = float(np.sqrt(np.mean(y * y)))
        processing_ms = (time.perf_counter() - started) * 1000.0

        out_bytes = self._float_to_pcm16_le(y)
        if len(out_bytes) != len(pcm_bytes):
            raise RuntimeError(
                f"output byte length mismatch: in={len(pcm_bytes)} out={len(out_bytes)}"
            )

        self.frame_count += 1
        return out_bytes, in_rms, out_rms, processing_ms

    def warmup(self) -> None:
        """Prime scipy/filter path so the first realtime chunk is not cold-started."""
        silent = b"\x00\x00" * (5292 // 2)
        self.process_pcm(silent)


def read_exact(stream, nbytes: int) -> bytes | None:
    """Read exactly nbytes from stream, or None on clean EOF before any data."""
    buf = bytearray()
    while len(buf) < nbytes:
        chunk = stream.read(nbytes - len(buf))
        if not chunk:
            if len(buf) == 0:
                return None
            raise EOFError(f"unexpected EOF after {len(buf)} of {nbytes} bytes")
        buf.extend(chunk)
    return bytes(buf)


def main() -> None:
    stdin = sys.stdin.buffer
    stdout = sys.stdout.buffer
    vocoder = StreamingVocoder()

    print("realtime vocoder started", file=sys.stderr)

    while True:
        header = read_exact(stdin, HEADER_SIZE)
        if header is None:
            break

        seq, pcm_length = struct.unpack(HEADER_FORMAT, header)
        if pcm_length < 0 or pcm_length > MAX_PCM_LENGTH:
            print(f"invalid pcm_length: {pcm_length}", file=sys.stderr)
            sys.exit(1)

        if seq == 0 and pcm_length == 0:
            if not vocoder.warmed_up:
                vocoder.warmup()
                vocoder.warmed_up = True
                print("realtime vocoder ready", file=sys.stderr)
            stdout.write(struct.pack(HEADER_FORMAT, 0, 0))
            stdout.flush()
            continue

        pcm = read_exact(stdin, pcm_length)
        if pcm is None:
            break

        try:
            out_pcm, in_rms, out_rms, processing_ms = vocoder.process_pcm(pcm)
        except Exception as exc:
            print(f"frame processing failed seq={seq}: {exc}", file=sys.stderr)
            sys.exit(1)

        if vocoder.frame_count % 100 == 0:
            print(
                "[realtime-vocoder]\n"
                f"frame={vocoder.frame_count}\n"
                f"inRms={in_rms:.6f}\n"
                f"outRms={out_rms:.6f}\n"
                f"processingMs={processing_ms:.2f}",
                file=sys.stderr,
            )

        stdout.write(struct.pack(HEADER_FORMAT, seq, len(out_pcm)))
        if out_pcm:
            stdout.write(out_pcm)
        stdout.flush()


if __name__ == "__main__":
    try:
        main()
    except BrokenPipeError:
        pass
