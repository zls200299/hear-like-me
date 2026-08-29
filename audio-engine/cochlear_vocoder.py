#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
cochlear_vocoder.py
===================

人工耳蜗（Cochlear Implant）声码器模拟 —— Python 版本。

这是网页版 "hear like me · Cochlear Vocoder Simulator" 中音频算法的等价实现。
原网页用 Web Audio API 的节点图完成处理，本文件用 numpy/scipy 把同一条链路
离线重建出来：

    输入 -> (+ 背景噪声) -> N 个通道 [ 带通 -> 全波整流 -> 包络低通 -> 乘以载体
                                      -> 输出带通 ] -> 求和 -> 动态压缩 -> 输出

关键参数与网页保持一致：
    - 频带边界按对数等分 (edges)
    - 每带 fc = sqrt(lo*hi)，Q = clamp(fc/bw, 0.5, 18)
    - 包络放大 envAmp = 2.6
    - 输出带通 Q = min(Q, 10)
    - 电流扩散 spread: 相邻通道 0.6*s，隔一个通道 0.28*s
    - wet = 0.9，压缩器 threshold=-16dB ratio=4 attack=3ms release=250ms
    - 滤波器使用 Web Audio 的 RBJ biquad 公式（注意 lowpass/highpass 的 Q 以 dB 计）

依赖: numpy, scipy （WAV 读写用标准库 wave，不需要 soundfile）

用法示例:
    python cochlear_vocoder.py --sample vowel --channels 8 -o out.wav
    python cochlear_vocoder.py -i speech.wav --scenario restaurant -o out.wav
    python cochlear_vocoder.py -i speech.wav --channels 4 --carrier sine --spread 40
    python cochlear_vocoder.py --list-scenarios
"""

from __future__ import annotations

import argparse
import json
import math
import sys
import wave
from dataclasses import dataclass, field
from pathlib import Path

import numpy as np
from scipy.signal import lfilter

# =====================================================================
# 1. Web Audio 风格的 biquad 滤波器
#    注意：Web Audio 规范中 lowpass / highpass 的 Q 参数以「分贝」解释，
#    而 bandpass 的 Q 是普通线性 Q。这里严格照抄规范，保证与网页一致。
# =====================================================================


def biquad_coeffs(kind: str, f0: float, q: float, sr: int):
    """返回归一化后的 (b, a) 系数，对应 Web Audio BiquadFilterNode。"""
    f0 = max(1.0, min(f0, sr * 0.5 - 1.0))
    w0 = 2.0 * math.pi * f0 / sr
    cos_w0 = math.cos(w0)
    sin_w0 = math.sin(w0)

    if kind in ("lowpass", "highpass"):
        alpha = sin_w0 / (2.0 * (10.0 ** (q / 20.0)))
    else:
        alpha = sin_w0 / (2.0 * max(q, 1e-6))

    if kind == "lowpass":
        b0 = (1.0 - cos_w0) / 2.0
        b1 = 1.0 - cos_w0
        b2 = (1.0 - cos_w0) / 2.0
    elif kind == "highpass":
        b0 = (1.0 + cos_w0) / 2.0
        b1 = -(1.0 + cos_w0)
        b2 = (1.0 + cos_w0) / 2.0
    elif kind == "bandpass":  # constant 0 dB peak gain
        b0 = alpha
        b1 = 0.0
        b2 = -alpha
    else:
        raise ValueError(f"unsupported filter type: {kind}")

    a0 = 1.0 + alpha
    a1 = -2.0 * cos_w0
    a2 = 1.0 - alpha
    b = np.array([b0, b1, b2], dtype=np.float64) / a0
    a = np.array([1.0, a1 / a0, a2 / a0], dtype=np.float64)
    return b, a


def biquad(x: np.ndarray, kind: str, f0: float, q: float, sr: int) -> np.ndarray:
    b, a = biquad_coeffs(kind, f0, q, sr)
    return lfilter(b, a, x)


def clamp(v, lo, hi):
    return max(lo, min(hi, v))


def log_edges(n: int, lo: float, hi: float) -> np.ndarray:
    """对数等分的频带边界，等价于网页里的 edges(N, lo, hi)。"""
    return np.exp(np.linspace(math.log(lo), math.log(hi), n + 1))


# =====================================================================
# 2. 配置
# =====================================================================


@dataclass
class VocoderConfig:
    n_channels: int = 8          # 电极/通道数
    f_lo: float = 150.0          # 分析频率下限
    f_hi: float = 7000.0         # 分析频率上限
    carrier: str = "noise"       # 'noise' 或 'sine'
    env_cut: float = 160.0       # 包络低通截止 (Hz)，越低越"耳蜗植入"
    spread: float = 0.15         # 电流扩散 0..1
    noise_level: float = 0.0     # 背景噪声 0..1
    env_amp: float = 2.6         # 包络放大（对应 envAmp.gain）
    wet: float = 0.9
    compress: bool = True
    normalize: float | None = 0.89  # 峰值归一化目标；None = 保持原始电平
    seed: int | None = None      # 噪声随机种子，便于复现


# 网页中的 6 个场景预设
SCENARIOS: dict[str, dict] = {
    "quiet":      dict(n_channels=8, f_lo=150, f_hi=7000, noise_level=0.00, spread=0.15, env_cut=160, carrier="noise", sample="vowel"),
    "restaurant": dict(n_channels=8, f_lo=150, f_hi=7000, noise_level=0.55, spread=0.40, env_cut=160, carrier="noise", sample="vowel"),
    "phone":      dict(n_channels=8, f_lo=300, f_hi=3400, noise_level=0.15, spread=0.10, env_cut=160, carrier="noise", sample="vowel"),
    "music":      dict(n_channels=8, f_lo=80,  f_hi=8000, noise_level=0.00, spread=0.25, env_cut=220, carrier="noise", sample="melody"),
    "tone":       dict(n_channels=8, f_lo=150, f_hi=7000, noise_level=0.10, spread=0.20, env_cut=120, carrier="noise", sample="tone"),
    "minimal":    dict(n_channels=4, f_lo=150, f_hi=7000, noise_level=0.00, spread=0.00, env_cut=160, carrier="noise", sample="vowel"),
}


# =====================================================================
# 3. 动态压缩器（近似 Web Audio DynamicsCompressorNode）
# =====================================================================


def compressor(x: np.ndarray, sr: int, threshold_db=-16.0, ratio=4.0,
               knee_db=30.0, attack=0.003, release=0.25) -> np.ndarray:
    """前馈式软拐点压缩器。Web Audio 的实现还带 6ms 前瞻，这里做简化近似。"""
    eps = 1e-9
    level_db = 20.0 * np.log10(np.abs(x) + eps)

    over = level_db - threshold_db
    gain_db = np.zeros_like(level_db)

    # 拐点区域（二次插值）与拐点以上（线性压缩）
    in_knee = (over > -knee_db / 2.0) & (over <= knee_db / 2.0)
    above = over > knee_db / 2.0
    k = over[in_knee] + knee_db / 2.0
    gain_db[in_knee] = -(1.0 - 1.0 / ratio) * (k * k) / (2.0 * knee_db)
    gain_db[above] = -(1.0 - 1.0 / ratio) * over[above]

    # 攻击/释放平滑（作用在增益衰减量上）
    a_att = math.exp(-1.0 / (attack * sr))
    a_rel = math.exp(-1.0 / (release * sr))
    smoothed = np.empty_like(gain_db)
    g = 0.0
    for i in range(gain_db.size):
        target = gain_db[i]
        coef = a_att if target < g else a_rel   # 增益下降=起音，回升=释放
        g = coef * g + (1.0 - coef) * target
        smoothed[i] = g

    return x * (10.0 ** (smoothed / 20.0))


# =====================================================================
# 3b. 视觉化电平（对齐 HTML Electrode array: RMS * 3.2）
# =====================================================================


def compute_visual_levels(
    signal: np.ndarray,
    sr: int,
    fps: int,
    analyser_size: int = 256,
) -> np.ndarray:
    """把单通道 modulated 信号转成随时间变化的 0~1 level 数组。"""
    x = np.asarray(signal, dtype=np.float64).ravel()
    n = x.size
    if n == 0 or fps <= 0 or sr <= 0:
        return np.zeros(0, dtype=np.float32)

    frame_count = int(math.ceil(n / sr * fps))
    if frame_count == 0:
        return np.zeros(0, dtype=np.float32)

    times = np.arange(frame_count, dtype=np.float64) / fps
    end_idx = np.clip(np.round(times * sr).astype(np.int64), 0, n - 1)
    start_idx = np.maximum(0, end_idx - analyser_size + 1)
    counts = (end_idx - start_idx + 1).astype(np.float64)

    sq = x * x
    cs = np.concatenate(([0.0], np.cumsum(sq)))
    mean_sq = (cs[end_idx + 1] - cs[start_idx]) / counts
    levels = np.clip(np.sqrt(mean_sq) * 3.2, 0.0, 1.0)
    return levels.astype(np.float32)


def write_visualization_json(path: str, visualization: dict) -> None:
    out = Path(path)
    out.parent.mkdir(parents=True, exist_ok=True)
    with out.open("w", encoding="utf-8") as f:
        json.dump(visualization, f, ensure_ascii=False, separators=(",", ":"))


# =====================================================================
# 4. 声码器主体
# =====================================================================


def make_babble_noise(n: int, sr: int, level: float, rng: np.random.Generator,
                      wobble: bool = False) -> np.ndarray:
    """背景'人声嘈杂'噪声：白噪 -> 250Hz 高通 -> 3200Hz 低通 -> 增益。"""
    if level <= 0:
        return np.zeros(n)
    v = rng.uniform(-1.0, 1.0, n)
    v = biquad(v, "highpass", 250.0, 0.4, sr)
    v = biquad(v, "lowpass", 3200.0, 0.4, sr)
    if wobble:  # 网页实时版本里 3.3Hz 的慢速起伏，使其更像多人交谈
        t = np.arange(n) / sr
        v = v * (0.65 + 0.35 * np.sin(2 * math.pi * 3.3 * t))
    return v * (level * 0.5)


def vocode(
    x: np.ndarray,
    sr: int,
    cfg: VocoderConfig,
    visualization_fps: int | None = None,
) -> np.ndarray | tuple[np.ndarray, dict]:
    """把单声道信号 x 处理成人工耳蜗模拟音。"""
    rng = np.random.default_rng(cfg.seed)
    x = np.asarray(x, dtype=np.float64).ravel()
    n = x.size
    visualize = visualization_fps is not None
    viz_fps = int(visualization_fps) if visualize else 0

    # --- 输入 = 干声 + 背景噪声（在分频之前混入，才符合真实场景） ---
    sig = x + make_babble_noise(n, sr, cfg.noise_level, rng)

    N = cfg.n_channels
    e = log_edges(N, cfg.f_lo, cfg.f_hi)
    frame_count = int(math.ceil(n / sr * viz_fps)) if visualize else 0
    visual_levels = (
        np.zeros((frame_count, N), dtype=np.float32) if visualize else None
    )

    # 所有通道共用同一份白噪声（对应网页里共享的 noiseBuf）
    noise = rng.uniform(-1.0, 1.0, n) if cfg.carrier == "noise" else None
    t = np.arange(n) / sr

    envs = np.zeros((N, n))
    carriers = np.zeros((N, n))
    fcs, qs = [], []

    for i in range(N):
        lo, hi = e[i], e[i + 1]
        fc = math.sqrt(lo * hi)
        bw = max(hi - lo, 1.0)
        q = clamp(fc / bw, 0.5, 18.0)
        fcs.append(fc)
        qs.append(q)

        # 分析带通 -> 全波整流 -> 包络低通 -> 放大
        band = biquad(sig, "bandpass", fc, q, sr)
        rect = np.abs(band)
        env = biquad(rect, "lowpass", cfg.env_cut, 0.5, sr)
        envs[i] = env * cfg.env_amp

        # 载体：带通白噪 或 正弦
        if cfg.carrier == "noise":
            carriers[i] = biquad(noise, "bandpass", fc, q, sr)
        else:
            carriers[i] = np.sin(2 * math.pi * fc * t)

    # --- 电流扩散：把每个通道的包络按比例泄漏到相邻通道 ---
    gains = envs.copy()
    if cfg.spread > 0:
        s = cfg.spread
        for i in range(N):
            for j, amt in ((i - 1, s * 0.60), (i + 1, s * 0.60),
                           (i - 2, s * 0.28), (i + 2, s * 0.28)):
                if 0 <= j < N and amt > 0:
                    gains[j] += envs[i] * amt

    # --- 调制 + 输出带通 + 求和 ---
    wet = np.zeros(n)
    for i in range(N):
        modulated = carriers[i] * gains[i]
        if visualize:
            visual_levels[:, i] = compute_visual_levels(modulated, sr, viz_fps)
        wet += biquad(modulated, "bandpass", fcs[i], min(qs[i], 10.0), sr)

    y = wet * cfg.wet
    if cfg.compress:
        y = compressor(y, sr)

    # 调制后整体电平偏低（网页里靠系统音量补偿），离线导出时做峰值归一化
    if cfg.normalize:
        peak = float(np.abs(y).max())
        if peak > 1e-9:
            y = y * (cfg.normalize / peak)

    if not visualize:
        return y

    bands = []
    for i in range(N):
        lo = float(e[i])
        hi = float(e[i + 1])
        bands.append({
            "index": i,
            "lo": lo,
            "hi": hi,
            "fc": float(math.sqrt(lo * hi)),
        })

    frames_q = np.round(visual_levels * 255.0).astype(np.int32)
    visualization = {
        "version": 1,
        "fps": viz_fps,
        "durationMs": int(round(n / sr * 1000)),
        "sampleRate": int(sr),
        "nChannels": N,
        "levelScale": 255,
        "order": "low-to-high",
        "bands": bands,
        "frames": frames_q.tolist(),
    }
    return y, visualization


# =====================================================================
# 5. 内置测试音（对应网页的 renderVowels / renderTones / renderMelody）
# =====================================================================


def _saw(phase: np.ndarray, f_max: float, sr: int) -> np.ndarray:
    """加法合成的带限锯齿波，避免混叠。phase 为累积相位（弧度）。"""
    k_max = max(1, int((sr / 2.0) / max(f_max, 1.0)))
    out = np.zeros_like(phase)
    for k in range(1, k_max + 1):
        out += np.sin(k * phase) / k
    return out * (2.0 / math.pi)


def _tri(phase: np.ndarray, f: float, sr: int) -> np.ndarray:
    k_max = max(1, int((sr / 2.0) / max(f, 1.0)))
    out = np.zeros_like(phase)
    for k in range(1, k_max + 1, 2):
        out += ((-1) ** ((k - 1) // 2)) * np.sin(k * phase) / (k * k)
    return out * (8.0 / (math.pi ** 2))


def _ramp_env(n: int, sr: int, points: list[tuple[float, float]],
              exp_tail: bool = False) -> np.ndarray:
    """按 (时间, 数值) 断点做线性（或末段指数）插值的包络。"""
    t = np.arange(n) / sr
    times = np.array([p[0] for p in points])
    vals = np.array([p[1] for p in points])
    env = np.interp(t, times, vals, left=vals[0], right=0.0)
    env[t > times[-1]] = 0.0
    if exp_tail:
        env = np.clip(env, 1e-4, None)
    return env


def _formant_chain(src: np.ndarray, sr: int, f1, f2, f3, gain=1.0) -> np.ndarray:
    """三个共振峰带通并联，模拟元音音色（对应 formantChain）。"""
    specs = ((f1, 8.0, 1.0), (f2, 10.0, 0.7), (f3, 12.0, 0.5))
    out = np.zeros_like(src)
    for f, q, g in specs:
        out += biquad(src, "bandpass", f, q, sr) * (g * gain)
    return out


def render_vowels(sr: int = 44100) -> np.ndarray:
    """依次发出 /a/ /i/ /u/ 三个元音。"""
    dur = 3.4
    n = int(sr * dur)
    V = {"a": (730, 1090, 2440), "i": (270, 2290, 3010), "u": (300, 870, 2240)}
    seq = (("a", 0.10), ("i", 1.20), ("u", 2.30))
    out = np.zeros(n)
    t = np.arange(n) / sr
    f0 = 125.0
    saw = _saw(2 * math.pi * f0 * t, f0, sr)
    for v, t0 in seq:
        env = _ramp_env(n, sr, [(t0, 0.0), (t0 + 0.06, 0.9),
                                (t0 + 0.80, 0.9), (t0 + 1.00, 0.0)])
        out += _formant_chain(saw * env, sr, *V[v])
    return out * 0.9


def render_tones(sr: int = 44100) -> np.ndarray:
    """一个升调、一个降调（普通话声调线索测试）。"""
    dur = 2.9
    n = int(sr * dur)
    t = np.arange(n) / sr
    out = np.zeros(n)
    for t0, fa, fb in ((0.15, 110.0, 175.0), (1.45, 180.0, 105.0)):
        # 基频在 0.7s 内线性滑动
        f = np.interp(t, [t0, t0 + 0.7], [fa, fb], left=fa, right=fb)
        phase = 2 * math.pi * np.cumsum(f) / sr
        saw = _saw(phase, max(fa, fb), sr)
        env = _ramp_env(n, sr, [(t0, 0.0), (t0 + 0.06, 0.9),
                                (t0 + 0.60, 0.9), (t0 + 0.80, 0.0)])
        out += _formant_chain(saw * env, sr, 730, 1090, 2440)
    return out * 0.9


def render_melody(sr: int = 44100) -> np.ndarray:
    """《小星星》片段，用来听音乐旋律在声码器下损失了多少。"""
    step = 0.3
    notes = [262, 262, 392, 392, 440, 440, 392,
             349, 349, 330, 330, 294, 294, 262]
    dur = 4.5
    n = int(sr * dur)
    out = np.zeros(n)
    for i, f in enumerate(notes):
        t0 = i * step
        i0, i1 = int(t0 * sr), min(n, int((t0 + step) * sr))
        if i1 <= i0:
            continue
        m = i1 - i0
        tt = np.arange(m) / sr
        tri = _tri(2 * math.pi * f * tt, f, sr)
        sine = np.sin(2 * math.pi * (f * 2) * tt) * 0.25
        # 0.02s 冲起，然后指数衰减到 0.08
        env = np.empty(m)
        a = max(1, int(0.02 * sr))
        env[:a] = np.linspace(1e-4, 0.9, a)
        rest = m - a
        if rest > 0:
            env[a:] = 0.9 * (0.08 / 0.9) ** (np.arange(rest) / max(rest, 1))
        out[i0:i1] += (tri + sine) * env
    return out * 0.6


SAMPLES = {"vowel": render_vowels, "tone": render_tones, "melody": render_melody}


# =====================================================================
# 6. WAV 读写（标准库，无额外依赖）
# =====================================================================


def read_wav(path: str) -> tuple[np.ndarray, int]:
    with wave.open(path, "rb") as w:
        sr = w.getframerate()
        nch = w.getnchannels()
        width = w.getsampwidth()
        raw = w.readframes(w.getnframes())
    dtype = {1: np.uint8, 2: np.int16, 4: np.int32}.get(width)
    if dtype is None:
        raise ValueError("仅支持 8/16/32 位 PCM WAV；请先转换格式。")
    data = np.frombuffer(raw, dtype=dtype).astype(np.float64)
    if width == 1:
        data = (data - 128.0) / 128.0
    else:
        data /= float(np.iinfo(dtype).max)
    if nch > 1:
        data = data.reshape(-1, nch).mean(axis=1)   # 混成单声道
    return data, sr


def write_wav(path: str, x: np.ndarray, sr: int) -> None:
    x = np.clip(np.asarray(x, dtype=np.float64), -1.0, 1.0)
    pcm = np.where(x < 0, x * 0x8000, x * 0x7FFF).astype(np.int16)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sr)
        w.writeframes(pcm.tobytes())


# =====================================================================
# 7. 可懂度估分（对应网页的 computeIntelligibility / iiGrade）
# =====================================================================


def intelligibility(cfg: VocoderConfig) -> dict:
    eff = cfg.n_channels * (1 - 0.5 * cfg.spread)
    spec_show = clamp(1 - 0.72 ** eff, 0, 1)
    spec = spec_show ** 1.6
    pitch = clamp(math.sqrt(max(0.0, cfg.env_cut - 20) / 480), 0, 1)
    if cfg.carrier == "sine":
        pitch = clamp(pitch + 0.12, 0, 1)
    noise_margin = clamp(1 - cfg.noise_level * 1.05, 0, 1)
    cover = clamp((math.log(cfg.f_hi) - math.log(cfg.f_lo)) /
                  (math.log(8000) - math.log(80)), 0.4, 1)
    core = clamp(spec * 0.90 + pitch * 0.08, 0, 1)
    s01 = clamp(core * (0.30 + 0.70 * noise_margin) * (0.78 + 0.22 * cover), 0, 1)
    score = round(s01 * 100)
    if score < 24:
        grade = "几乎听不懂"
    elif score < 44:
        grade = "很吃力"
    elif score < 66:
        grade = "大致能懂"
    elif score < 86:
        grade = "比较清楚"
    else:
        grade = "接近清晰"
    return {"score": score, "grade": grade, "spectral": spec_show,
            "pitch": pitch, "noise_margin": noise_margin}


# =====================================================================
# 8. 命令行
# =====================================================================


def main(argv=None) -> int:
    p = argparse.ArgumentParser(
        description="人工耳蜗声码器模拟（网页版算法的 Python 实现）")
    p.add_argument("-i", "--input", help="输入 WAV 文件（16 位 PCM 最稳妥）")
    p.add_argument("-s", "--sample", choices=list(SAMPLES),
                   help="不给输入文件时，使用内置测试音: vowel / tone / melody")
    p.add_argument("-o", "--output", default="cochlear_out.wav", help="输出 WAV 路径")
    p.add_argument("--scenario", choices=list(SCENARIOS), help="使用场景预设")
    p.add_argument("-n", "--channels", type=int, help="通道（电极）数，2–22")
    p.add_argument("--carrier", choices=("noise", "sine"), help="载体类型")
    p.add_argument("--lo", type=float, help="频率下限 Hz")
    p.add_argument("--hi", type=float, help="频率上限 Hz")
    p.add_argument("--env-cut", type=float, help="包络低通截止 Hz (20–500)")
    p.add_argument("--spread", type=float, help="电流扩散百分比 0–100")
    p.add_argument("--noise", type=float, help="背景噪声百分比 0–100")
    p.add_argument("--no-compress", action="store_true", help="跳过输出端压缩器")
    p.add_argument("--no-normalize", action="store_true", help="不做峰值归一化（保持原始电平）")
    p.add_argument("--seed", type=int, help="噪声随机种子")
    p.add_argument("--visualization-json", help="输出 visualization JSON 路径")
    p.add_argument("--visualization-fps", type=int, default=30,
                   help="visualization 帧率 (10–60，默认 30)")
    p.add_argument("--list-scenarios", action="store_true", help="列出场景预设后退出")
    p.add_argument("--original-only", action="store_true",
                   help="只生成内置示例原声，不做声码器处理（仅 --sample 模式）")
    a = p.parse_args(argv)

    if a.list_scenarios:
        for k, v in SCENARIOS.items():
            print(f"{k:11s} {v}")
        return 0

    if a.original_only:
        if a.input:
            print("--original-only 仅支持 --sample 模式", file=sys.stderr)
            return 1
        if not a.sample:
            print("--original-only 需要指定 --sample", file=sys.stderr)
            return 1
        sr = 44100
        x = SAMPLES[a.sample](sr)
        write_wav(a.output, x, sr)
        print(f"原始示例音已保存: {a.output}")
        return 0

    cfg = VocoderConfig()
    sample_name = a.sample or "vowel"

    if a.scenario:
        s = dict(SCENARIOS[a.scenario])
        sample_name = a.sample or s.pop("sample")
        s.pop("sample", None)
        for k, v in s.items():
            setattr(cfg, k, v)

    if a.channels is not None:
        cfg.n_channels = int(clamp(a.channels, 1, 32))
    if a.carrier:
        cfg.carrier = a.carrier
    if a.lo:
        cfg.f_lo = a.lo
    if a.hi:
        cfg.f_hi = a.hi
    if a.env_cut is not None:
        cfg.env_cut = clamp(a.env_cut, 20, 500)
    if a.spread is not None:
        cfg.spread = clamp(a.spread / 100.0, 0, 1)
    if a.noise is not None:
        cfg.noise_level = clamp(a.noise / 100.0, 0, 1)
    cfg.compress = not a.no_compress
    cfg.normalize = None if a.no_normalize else 0.89
    cfg.seed = a.seed

    if a.input:
        x, sr = read_wav(a.input)
        src_tag = "file"
    else:
        sr = 44100
        x = SAMPLES[sample_name](sr)
        src_tag = sample_name
        write_wav(a.output.replace(".wav", "_original.wav"), x, sr)
        print(f"原始参考音已保存: {a.output.replace('.wav', '_original.wav')}")

    viz_path = a.visualization_json
    viz_fps = int(clamp(a.visualization_fps, 10, 60)) if viz_path else None
    if viz_fps is not None:
        y, visualization = vocode(x, sr, cfg, visualization_fps=viz_fps)
        write_visualization_json(viz_path, visualization)
    else:
        y = vocode(x, sr, cfg)

    write_wav(a.output, y, sr)

    ii = intelligibility(cfg)
    print(f"已输出: {a.output}")
    print(f"  来源: {src_tag} · {len(x)/sr:.2f}s @ {sr} Hz")
    print(f"  通道: {cfg.n_channels} · 载体: {cfg.carrier} · "
          f"{cfg.f_lo:.0f}–{cfg.f_hi:.0f} Hz")
    print(f"  包络截止: {cfg.env_cut:.0f} Hz · 扩散: {cfg.spread*100:.0f}% · "
          f"噪声: {cfg.noise_level*100:.0f}%")
    print(f"  可懂度估分: {ii['score']}/100 ({ii['grade']})")
    print(f"CLARITY_SCORE={ii['score']}")
    if viz_fps is not None:
        print(f"VISUALIZATION_FPS={visualization['fps']}")
        print(f"VISUALIZATION_FRAMES={len(visualization['frames'])}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
