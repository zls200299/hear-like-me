#!/usr/bin/env python3
"""Realtime streaming cochlear vocoder (stdin/stdout binary framing)."""

from __future__ import annotations

import json
import math
import struct
import sys
import time
from dataclasses import dataclass
from typing import Any

import numpy as np
from scipy.signal import lfilter

from cochlear_vocoder import biquad_coeffs, clamp, log_edges

MAX_PCM_LENGTH = 1024 * 1024
HEADER_FORMAT = ">II"
HEADER_SIZE = struct.calcsize(HEADER_FORMAT)
CROSSFADE_FRAMES = 2
LEVEL_ANALYSER_SIZE = 256
LEVEL_GAIN = 3.2

SAMPLE_RATE = 44100
ENV_AMP = 2.6
WET = 0.9
OUTPUT_GAIN = 10.0
COMPRESS_THRESHOLD_DB = -16.0
COMPRESS_RATIO = 4.0
COMPRESS_KNEE_DB = 30.0
COMPRESS_ATTACK = 0.003
COMPRESS_RELEASE = 0.25


@dataclass
class RealtimeConfig:
    n_channels: int = 8
    f_lo: float = 150.0
    f_hi: float = 7000.0
    carrier: str = "noise"
    env_cut: float = 160.0
    spread: float = 0.15
    noise_level: float = 0.0

    def structural_key(self) -> tuple[Any, ...]:
        return (
            self.n_channels,
            self.f_lo,
            self.f_hi,
            self.env_cut,
            self.carrier,
        )

    def copy(self) -> RealtimeConfig:
        return RealtimeConfig(
            n_channels=self.n_channels,
            f_lo=self.f_lo,
            f_hi=self.f_hi,
            carrier=self.carrier,
            env_cut=self.env_cut,
            spread=self.spread,
            noise_level=self.noise_level,
        )

    @classmethod
    def from_params(cls, params: dict[str, Any]) -> RealtimeConfig:
        def pick(*keys: str, default: Any = None) -> Any:
            for key in keys:
                if key in params and params[key] is not None:
                    return params[key]
            return default

        carrier = str(pick("carrier", default="noise")).strip().lower()
        if carrier not in ("noise", "sine"):
            raise ValueError(f"invalid carrier: {carrier}")

        n_channels = int(pick("nChannels", "n_channels", default=8))
        f_lo = float(pick("fLo", "f_lo", default=150.0))
        f_hi = float(pick("fHi", "f_hi", default=7000.0))
        env_cut = float(pick("envCut", "env_cut", default=160.0))
        spread = float(pick("spread", default=0.15))
        noise_level = float(pick("noiseLevel", "noise_level", default=0.0))

        if n_channels < 1 or n_channels > 22:
            raise ValueError(f"nChannels out of range: {n_channels}")
        if f_lo <= 0 or f_hi <= f_lo:
            raise ValueError(f"invalid frequency range: {f_lo}-{f_hi}")
        if f_hi >= SAMPLE_RATE / 2:
            raise ValueError(f"fHi must be below Nyquist ({SAMPLE_RATE / 2:.0f} Hz)")
        if env_cut < 20 or env_cut > 500:
            raise ValueError(f"envCut out of range: {env_cut}")
        if spread < 0 or spread > 1:
            raise ValueError(f"spread out of range: {spread}")
        if noise_level < 0 or noise_level > 1:
            raise ValueError(f"noiseLevel out of range: {noise_level}")

        return cls(
            n_channels=n_channels,
            f_lo=f_lo,
            f_hi=f_hi,
            carrier=carrier,
            env_cut=env_cut,
            spread=spread,
            noise_level=noise_level,
        )


class StatefulBiquad:
    def __init__(self, kind: str, f0: float, q: float, sr: int):
        self.b, self.a = biquad_coeffs(kind, f0, q, sr)
        self.zi = np.zeros(2, dtype=np.float64)

    def process(self, x: np.ndarray) -> np.ndarray:
        y, self.zi = lfilter(self.b, self.a, x, zi=self.zi)
        return y


class StatefulBabbleNoise:
    def __init__(self, sr: int, rng: np.random.Generator):
        self.sr = sr
        self.rng = rng
        self.hp = StatefulBiquad("highpass", 250.0, 0.4, sr)
        self.lp = StatefulBiquad("lowpass", 3200.0, 0.4, sr)
        self.sample_offset = 0

    def process(self, n: int, level: float) -> np.ndarray:
        if level <= 0 or n <= 0:
            return np.zeros(n, dtype=np.float64)

        v = self.rng.uniform(-1.0, 1.0, n)
        v = self.hp.process(v)
        v = self.lp.process(v)
        t = (np.arange(n, dtype=np.float64) + self.sample_offset) / self.sr
        v = v * (0.65 + 0.35 * np.sin(2.0 * math.pi * 3.3 * t))
        self.sample_offset += n
        return v * (level * 0.5)


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
    def __init__(
        self,
        config: RealtimeConfig,
        rng: np.random.Generator,
        compressor_gain_db: float = 0.0,
    ):
        self.config = config.copy()
        self.sample_rate = SAMPLE_RATE
        self.compressor_gain_db = compressor_gain_db
        self.rng = rng
        self.babble = StatefulBabbleNoise(SAMPLE_RATE, rng)
        self.channels: list[ChannelState] = []
        self._rebuild_channels()

    def _rebuild_channels(self) -> None:
        cfg = self.config
        edges = log_edges(cfg.n_channels, cfg.f_lo, cfg.f_hi)
        self.channels = []
        for i in range(cfg.n_channels):
            lo, hi = float(edges[i]), float(edges[i + 1])
            fc = math.sqrt(lo * hi)
            bw = max(hi - lo, 1.0)
            q = clamp(fc / bw, 0.5, 18.0)
            self.channels.append(ChannelState(fc, q, cfg.env_cut, SAMPLE_RATE, cfg.carrier))

    def _apply_spread(self, envs: np.ndarray) -> np.ndarray:
        gains = envs.copy()
        s = self.config.spread
        if s <= 0:
            return gains

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
    def _chunk_levels_from_modulated(
        modulated_signals: list[np.ndarray],
        analyser_size: int = LEVEL_ANALYSER_SIZE,
        level_gain: float = LEVEL_GAIN,
    ) -> list[int]:
        levels: list[int] = []
        for modulated in modulated_signals:
            tail = modulated[-analyser_size:] if modulated.size > analyser_size else modulated
            if tail.size == 0:
                levels.append(0)
                continue
            rms = float(np.sqrt(np.mean(tail * tail)))
            level = clamp(rms * level_gain, 0.0, 1.0)
            levels.append(int(round(level * 255.0)))
        return levels

    def process_samples(self, x: np.ndarray) -> tuple[np.ndarray, list[int]]:
        n = x.size
        cfg = self.config
        sig = x + self.babble.process(n, cfg.noise_level)
        noise = self.rng.uniform(-1.0, 1.0, n) if cfg.carrier == "noise" else None

        envs = np.zeros((cfg.n_channels, n), dtype=np.float64)
        carriers = np.zeros((cfg.n_channels, n), dtype=np.float64)
        for i, channel in enumerate(self.channels):
            band = channel.analysis_bp.process(sig)
            rect = np.abs(band)
            env = channel.envelope_lp.process(rect)
            envs[i] = env * ENV_AMP
            carriers[i] = channel.carrier_signal(noise, n, self.sample_rate)

        gains = self._apply_spread(envs)

        wet = np.zeros(n, dtype=np.float64)
        modulated_signals: list[np.ndarray] = []
        for i, channel in enumerate(self.channels):
            modulated = carriers[i] * gains[i]
            modulated_signals.append(modulated)
            wet += channel.output_bp.process(modulated)

        y = wet * WET
        y = self._compress(y)
        levels = self._chunk_levels_from_modulated(modulated_signals)
        return y * OUTPUT_GAIN, levels

    @staticmethod
    def _float_to_pcm16_le(y: np.ndarray) -> bytes:
        clipped = np.clip(np.asarray(y, dtype=np.float64), -1.0, 1.0)
        pcm = np.where(clipped < 0, clipped * 0x8000, clipped * 0x7FFF).astype("<i2")
        return pcm.tobytes()


class _CrossfadeTransition:
    def __init__(self, old_engine: StreamingVocoder, new_engine: StreamingVocoder):
        self.old_engine = old_engine
        self.new_engine = new_engine
        self.frames_done = 0


class RealtimeVocoderSession:
    def __init__(self, seed: int | None = None):
        self.rng = np.random.default_rng(seed)
        self.config = RealtimeConfig()
        self.engine = StreamingVocoder(self.config, self.rng)
        self.warmed_up = False
        self.frame_count = 0
        self._crossfade: _CrossfadeTransition | None = None

    def warmup(self) -> None:
        silent = b"\x00\x00" * (5292 // 2)
        self.process_pcm(silent)
        self.warmed_up = True

    def handle_control_message(self, raw: bytes) -> bytes:
        try:
            msg = json.loads(raw.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            return self._error_response(0, f"invalid JSON: {exc}")

        version = msg.get("version", 0)

        if msg.get("type") != "PARAM_UPDATE":
            return self._error_response(version, "unsupported control type")

        params = msg.get("params")
        if not isinstance(params, dict):
            return self._error_response(version, "params must be an object")

        try:
            new_config = RealtimeConfig.from_params(params)
            self._apply_config(new_config)
        except ValueError as exc:
            return self._error_response(version, str(exc))

        print(
            "[realtime-vocoder] params applied "
            f"version={version} "
            f"n={new_config.n_channels} "
            f"{new_config.f_lo:.0f}-{new_config.f_hi:.0f}Hz "
            f"carrier={new_config.carrier} "
            f"spread={new_config.spread:.2f} "
            f"noise={new_config.noise_level:.2f}",
            file=sys.stderr,
        )
        return json.dumps(
            {"type": "PARAM_APPLIED", "version": version},
            separators=(",", ":"),
        ).encode("utf-8")

    def _apply_light_params(self, new_config: RealtimeConfig) -> None:
        self.config.spread = new_config.spread
        self.config.noise_level = new_config.noise_level
        self.engine.config.spread = new_config.spread
        self.engine.config.noise_level = new_config.noise_level
        if self._crossfade is not None:
            self._crossfade.old_engine.config.spread = new_config.spread
            self._crossfade.old_engine.config.noise_level = new_config.noise_level
            self._crossfade.new_engine.config.spread = new_config.spread
            self._crossfade.new_engine.config.noise_level = new_config.noise_level

    def _apply_config(self, new_config: RealtimeConfig) -> None:
        if new_config.structural_key() != self.config.structural_key():
            new_engine = StreamingVocoder(
                new_config,
                self.rng,
                compressor_gain_db=self.engine.compressor_gain_db,
            )
            self._crossfade = _CrossfadeTransition(self.engine, new_engine)
            self.config = new_config.copy()
            return

        self._apply_light_params(new_config)

    @staticmethod
    def _error_response(version: int, message: str) -> bytes:
        payload = {
            "type": "PARAM_ERROR",
            "version": version,
            "message": message,
        }
        return json.dumps(payload, separators=(",", ":")).encode("utf-8")

    def process_pcm(
        self, pcm_bytes: bytes
    ) -> tuple[bytes, list[int], float, float, float, float, float]:
        if len(pcm_bytes) % 2 != 0:
            raise ValueError(f"PCM byte length must be even, got {len(pcm_bytes)}")

        if len(pcm_bytes) == 0:
            return b"", [], 0.0, 0.0, 0.0, 0.0, 0.0

        started = time.perf_counter()
        x = np.frombuffer(pcm_bytes, dtype="<i2").astype(np.float64) / 32768.0
        in_rms = float(np.sqrt(np.mean(x * x)))

        if self._crossfade is not None:
            old_y, _ = self._crossfade.old_engine.process_samples(x)
            new_y, levels = self._crossfade.new_engine.process_samples(x)
            alpha = (self._crossfade.frames_done + 1) / CROSSFADE_FRAMES
            y = (1.0 - alpha) * old_y + alpha * new_y
            self._crossfade.frames_done += 1
            if self._crossfade.frames_done >= CROSSFADE_FRAMES:
                self.engine = self._crossfade.new_engine
                self._crossfade = None
        else:
            y, levels = self.engine.process_samples(x)

        out_rms = float(np.sqrt(np.mean(y * y)))
        peak = float(np.max(np.abs(y)))
        clip_ratio = float(np.mean(np.abs(y) > 1.0))
        processing_ms = (time.perf_counter() - started) * 1000.0

        out_bytes = StreamingVocoder._float_to_pcm16_le(y)
        if len(out_bytes) != len(pcm_bytes):
            raise RuntimeError(
                f"output byte length mismatch: in={len(pcm_bytes)} out={len(out_bytes)}"
            )

        self.frame_count += 1
        return out_bytes, levels, in_rms, out_rms, processing_ms, peak, clip_ratio


def build_audio_frame_payload(pcm_bytes: bytes, levels: list[int]) -> bytes:
    channel_count = len(levels)
    if channel_count < 0 or channel_count > 255:
        raise ValueError(f"invalid channel count for levels: {channel_count}")
    return (
        struct.pack(">I", len(pcm_bytes))
        + pcm_bytes
        + struct.pack("B", channel_count)
        + bytes(levels)
    )


def read_exact(stream, nbytes: int) -> bytes | None:
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
    session = RealtimeVocoderSession()

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
            if not session.warmed_up:
                session.warmup()
                print("realtime vocoder ready", file=sys.stderr)
            stdout.write(struct.pack(HEADER_FORMAT, 0, 0))
            stdout.flush()
            continue

        if seq == 0 and pcm_length > 0:
            control_raw = read_exact(stdin, pcm_length)
            if control_raw is None:
                break
            response = session.handle_control_message(control_raw)
            stdout.write(struct.pack(HEADER_FORMAT, 0, len(response)))
            stdout.write(response)
            stdout.flush()
            continue

        pcm = read_exact(stdin, pcm_length)
        if pcm is None:
            break

        try:
            out_pcm, levels, in_rms, out_rms, processing_ms, peak, clip_ratio = session.process_pcm(
                pcm
            )
        except Exception as exc:
            print(f"frame processing failed seq={seq}: {exc}", file=sys.stderr)
            sys.exit(1)

        if session.frame_count % 100 == 0:
            print(
                "[realtime-vocoder]\n"
                f"frame={session.frame_count}\n"
                f"inRms={in_rms:.6f}\n"
                f"outRms={out_rms:.6f}\n"
                f"peak={peak:.6f}\n"
                f"clipRatio={clip_ratio:.6f}\n"
                f"processingMs={processing_ms:.2f}\n"
                f"levels={levels}",
                file=sys.stderr,
            )

        payload = build_audio_frame_payload(out_pcm, levels)
        stdout.write(struct.pack(HEADER_FORMAT, seq, len(payload)))
        stdout.write(payload)
        stdout.flush()


if __name__ == "__main__":
    try:
        main()
    except BrokenPipeError:
        pass
