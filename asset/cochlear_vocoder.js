#!/usr/bin/env node
/**
 * cochlear_vocoder.js
 * ===================
 *
 * 人工耳蜗（Cochlear Implant）声码器模拟 —— JavaScript 版本。
 *
 * 百分百还原自 cochlear_vocoder.py：同一条离线处理链、同一套 Web Audio RBJ
 * biquad 系数、同一套 scipy.signal.lfilter（Direct Form II Transposed）、
 * 同一压缩器/场景/内置测试音/可懂度估分。
 *
 *     输入 -> (+ 背景噪声) -> N 个通道 [ 带通 -> 全波整流 -> 包络低通 -> 乘以载体
 *                                       -> 输出带通 ] -> 求和 -> 动态压缩 -> 输出
 *
 * 依赖: 仅 Node.js 内置模块（fs / path / zlib 不需要）；浏览器可直接用导出的
 *       vocode / renderVowels 等（忽略 CLI 与 WAV 读写）。
 *
 * 用法示例:
 *     node cochlear_vocoder.js --sample vowel --channels 8 -o out.wav
 *     node cochlear_vocoder.js -i speech.wav --scenario restaurant -o out.wav
 *     node cochlear_vocoder.js -i speech.wav --channels 4 --carrier sine --spread 40
 *     node cochlear_vocoder.js --list-scenarios
 *
 * 注意: 含噪声时随机序列与 Python numpy PCG64 不同（算法路径一致）；
 *       --carrier sine 且 --noise 0 时，输出应与 Python 数值对齐。
 */

'use strict';

const fs = require('fs');
const path = require('path');

// =====================================================================
// 1. Web Audio 风格的 biquad 滤波器
//    注意：Web Audio 规范中 lowpass / highpass 的 Q 参数以「分贝」解释，
//    而 bandpass 的 Q 是普通线性 Q。这里严格照抄规范，保证与网页/Python 一致。
// =====================================================================

function biquadCoeffs(kind, f0, q, sr) {
  f0 = Math.max(1.0, Math.min(f0, sr * 0.5 - 1.0));
  const w0 = (2.0 * Math.PI * f0) / sr;
  const cosW0 = Math.cos(w0);
  const sinW0 = Math.sin(w0);

  let alpha;
  if (kind === 'lowpass' || kind === 'highpass') {
    alpha = sinW0 / (2.0 * Math.pow(10.0, q / 20.0));
  } else {
    alpha = sinW0 / (2.0 * Math.max(q, 1e-6));
  }

  let b0, b1, b2;
  if (kind === 'lowpass') {
    b0 = (1.0 - cosW0) / 2.0;
    b1 = 1.0 - cosW0;
    b2 = (1.0 - cosW0) / 2.0;
  } else if (kind === 'highpass') {
    b0 = (1.0 + cosW0) / 2.0;
    b1 = -(1.0 + cosW0);
    b2 = (1.0 + cosW0) / 2.0;
  } else if (kind === 'bandpass') {
    // constant 0 dB peak gain
    b0 = alpha;
    b1 = 0.0;
    b2 = -alpha;
  } else {
    throw new Error(`unsupported filter type: ${kind}`);
  }

  const a0 = 1.0 + alpha;
  const a1 = -2.0 * cosW0;
  const a2 = 1.0 - alpha;
  return {
    b: [b0 / a0, b1 / a0, b2 / a0],
    a: [1.0, a1 / a0, a2 / a0],
  };
}

/** scipy.signal.lfilter Direct Form II Transposed，阶数 2（biquad）。 */
function lfilter(b, a, x) {
  const n = x.length;
  const y = new Float64Array(n);
  const b0 = b[0], b1 = b[1], b2 = b[2];
  const a1 = a[1], a2 = a[2];
  let z0 = 0.0, z1 = 0.0;
  for (let i = 0; i < n; i++) {
    const xi = x[i];
    const yi = b0 * xi + z0;
    z0 = b1 * xi - a1 * yi + z1;
    z1 = b2 * xi - a2 * yi;
    y[i] = yi;
  }
  return y;
}

function biquad(x, kind, f0, q, sr) {
  const { b, a } = biquadCoeffs(kind, f0, q, sr);
  return lfilter(b, a, x);
}

function clamp(v, lo, hi) {
  return Math.max(lo, Math.min(hi, v));
}

/** 对数等分的频带边界，等价于网页/Python 的 edges(N, lo, hi)。 */
function logEdges(n, lo, hi) {
  const out = new Float64Array(n + 1);
  const logLo = Math.log(lo);
  const logHi = Math.log(hi);
  for (let i = 0; i <= n; i++) {
    out[i] = Math.exp(logLo + ((logHi - logLo) * i) / n);
  }
  return out;
}

// =====================================================================
// 2. 可复现 RNG（Mulberry32）。算法路径与 Python 一致；噪声样本序列不同。
//    无 seed 时用 Math.random 填充，与 Python seed=None 行为同类。
// =====================================================================

function makeRng(seed) {
  if (seed == null) {
    return {
      uniform(lo, hi, n) {
        const out = new Float64Array(n);
        const span = hi - lo;
        for (let i = 0; i < n; i++) out[i] = lo + Math.random() * span;
        return out;
      },
    };
  }
  let s = (seed >>> 0) || 1;
  const next = () => {
    s = (s + 0x6d2b79f5) >>> 0;
    let t = Math.imul(s ^ (s >>> 15), 1 | s);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
  return {
    uniform(lo, hi, n) {
      const out = new Float64Array(n);
      const span = hi - lo;
      for (let i = 0; i < n; i++) out[i] = lo + next() * span;
      return out;
    },
  };
}

// =====================================================================
// 3. 配置
// =====================================================================

function VocoderConfig(overrides) {
  const cfg = {
    nChannels: 8,
    fLo: 150.0,
    fHi: 7000.0,
    carrier: 'noise',
    envCut: 160.0,
    spread: 0.15,
    noiseLevel: 0.0,
    envAmp: 2.6,
    wet: 0.9,
    compress: true,
    normalize: 0.89, // null = 保持原始电平
    seed: null,
  };
  if (overrides) Object.assign(cfg, overrides);
  return cfg;
}

/** 网页中的 6 个场景预设（字段名与 Python SCENARIOS 对齐后再映射到 JS）。 */
const SCENARIOS = {
  quiet:      { nChannels: 8, fLo: 150, fHi: 7000, noiseLevel: 0.0,  spread: 0.15, envCut: 160, carrier: 'noise', sample: 'vowel' },
  restaurant: { nChannels: 8, fLo: 150, fHi: 7000, noiseLevel: 0.55, spread: 0.4,  envCut: 160, carrier: 'noise', sample: 'vowel' },
  phone:      { nChannels: 8, fLo: 300, fHi: 3400, noiseLevel: 0.15, spread: 0.1,  envCut: 160, carrier: 'noise', sample: 'vowel' },
  music:      { nChannels: 8, fLo: 80,  fHi: 8000, noiseLevel: 0.0,  spread: 0.25, envCut: 220, carrier: 'noise', sample: 'melody' },
  tone:       { nChannels: 8, fLo: 150, fHi: 7000, noiseLevel: 0.1,  spread: 0.2,  envCut: 120, carrier: 'noise', sample: 'tone' },
  minimal:    { nChannels: 4, fLo: 150, fHi: 7000, noiseLevel: 0.0,  spread: 0.0,  envCut: 160, carrier: 'noise', sample: 'vowel' },
};

// =====================================================================
// 4. 动态压缩器（近似 Web Audio DynamicsCompressorNode）
// =====================================================================

function compressor(x, sr, thresholdDb = -16.0, ratio = 4.0,
                    kneeDb = 30.0, attack = 0.003, release = 0.25) {
  const eps = 1e-9;
  const n = x.length;
  const gainDb = new Float64Array(n);
  const halfKnee = kneeDb / 2.0;
  const slope = 1.0 - 1.0 / ratio;

  for (let i = 0; i < n; i++) {
    const levelDb = 20.0 * Math.log10(Math.abs(x[i]) + eps);
    const over = levelDb - thresholdDb;
    if (over > -halfKnee && over <= halfKnee) {
      const k = over + halfKnee;
      gainDb[i] = -slope * (k * k) / (2.0 * kneeDb);
    } else if (over > halfKnee) {
      gainDb[i] = -slope * over;
    } else {
      gainDb[i] = 0.0;
    }
  }

  const aAtt = Math.exp(-1.0 / (attack * sr));
  const aRel = Math.exp(-1.0 / (release * sr));
  const out = new Float64Array(n);
  let g = 0.0;
  for (let i = 0; i < n; i++) {
    const target = gainDb[i];
    const coef = target < g ? aAtt : aRel;
    g = coef * g + (1.0 - coef) * target;
    out[i] = x[i] * Math.pow(10.0, g / 20.0);
  }
  return out;
}

// =====================================================================
// 5. 声码器主体
// =====================================================================

function makeBabbleNoise(n, sr, level, rng, wobble = false) {
  if (level <= 0) return new Float64Array(n);
  let v = rng.uniform(-1.0, 1.0, n);
  v = biquad(v, 'highpass', 250.0, 0.4, sr);
  v = biquad(v, 'lowpass', 3200.0, 0.4, sr);
  if (wobble) {
    for (let i = 0; i < n; i++) {
      const t = i / sr;
      v[i] *= 0.65 + 0.35 * Math.sin(2 * Math.PI * 3.3 * t);
    }
  }
  const scale = level * 0.5;
  for (let i = 0; i < n; i++) v[i] *= scale;
  return v;
}

function toFloat64Mono(x) {
  if (x instanceof Float64Array) return x;
  const out = new Float64Array(x.length);
  for (let i = 0; i < x.length; i++) out[i] = x[i];
  return out;
}

/** 把单声道信号 x 处理成人工耳蜗模拟音。 */
function vocode(x, sr, cfg) {
  cfg = cfg || VocoderConfig();
  const rng = makeRng(cfg.seed);
  x = toFloat64Mono(x);
  const n = x.length;

  const noiseBg = makeBabbleNoise(n, sr, cfg.noiseLevel, rng);
  const sig = new Float64Array(n);
  for (let i = 0; i < n; i++) sig[i] = x[i] + noiseBg[i];

  const N = cfg.nChannels;
  const e = logEdges(N, cfg.fLo, cfg.fHi);

  const noise = cfg.carrier === 'noise' ? rng.uniform(-1.0, 1.0, n) : null;

  const envs = Array.from({ length: N }, () => new Float64Array(n));
  const carriers = Array.from({ length: N }, () => new Float64Array(n));
  const fcs = new Float64Array(N);
  const qs = new Float64Array(N);

  for (let i = 0; i < N; i++) {
    const lo = e[i], hi = e[i + 1];
    const fc = Math.sqrt(lo * hi);
    const bw = Math.max(hi - lo, 1.0);
    const q = clamp(fc / bw, 0.5, 18.0);
    fcs[i] = fc;
    qs[i] = q;

    const band = biquad(sig, 'bandpass', fc, q, sr);
    const rect = new Float64Array(n);
    for (let j = 0; j < n; j++) rect[j] = Math.abs(band[j]);
    const env = biquad(rect, 'lowpass', cfg.envCut, 0.5, sr);
    for (let j = 0; j < n; j++) envs[i][j] = env[j] * cfg.envAmp;

    if (cfg.carrier === 'noise') {
      carriers[i] = biquad(noise, 'bandpass', fc, q, sr);
    } else {
      for (let j = 0; j < n; j++) {
        carriers[i][j] = Math.sin(2 * Math.PI * fc * (j / sr));
      }
    }
  }

  const gains = envs.map((row) => new Float64Array(row));
  if (cfg.spread > 0) {
    const s = cfg.spread;
    for (let i = 0; i < N; i++) {
      const leaks = [
        [i - 1, s * 0.6],
        [i + 1, s * 0.6],
        [i - 2, s * 0.28],
        [i + 2, s * 0.28],
      ];
      for (const [j, amt] of leaks) {
        if (j >= 0 && j < N && amt > 0) {
          for (let k = 0; k < n; k++) gains[j][k] += envs[i][k] * amt;
        }
      }
    }
  }

  let wet = new Float64Array(n);
  for (let i = 0; i < N; i++) {
    const modulated = new Float64Array(n);
    for (let k = 0; k < n; k++) modulated[k] = carriers[i][k] * gains[i][k];
    const filtered = biquad(modulated, 'bandpass', fcs[i], Math.min(qs[i], 10.0), sr);
    for (let k = 0; k < n; k++) wet[k] += filtered[k];
  }

  let y = new Float64Array(n);
  for (let i = 0; i < n; i++) y[i] = wet[i] * cfg.wet;
  if (cfg.compress) y = compressor(y, sr);

  if (cfg.normalize != null) {
    let peak = 0.0;
    for (let i = 0; i < n; i++) {
      const a = Math.abs(y[i]);
      if (a > peak) peak = a;
    }
    if (peak > 1e-9) {
      const scale = cfg.normalize / peak;
      for (let i = 0; i < n; i++) y[i] *= scale;
    }
  }
  return y;
}

// =====================================================================
// 6. 内置测试音
// =====================================================================

function saw(phase, fMax, sr) {
  const kMax = Math.max(1, Math.floor((sr / 2.0) / Math.max(fMax, 1.0)));
  const out = new Float64Array(phase.length);
  for (let k = 1; k <= kMax; k++) {
    for (let i = 0; i < phase.length; i++) {
      out[i] += Math.sin(k * phase[i]) / k;
    }
  }
  const scale = 2.0 / Math.PI;
  for (let i = 0; i < out.length; i++) out[i] *= scale;
  return out;
}

function tri(phase, f, sr) {
  const kMax = Math.max(1, Math.floor((sr / 2.0) / Math.max(f, 1.0)));
  const out = new Float64Array(phase.length);
  for (let k = 1; k <= kMax; k += 2) {
    // (-1)^((k-1)//2) — 与 Python 一致
    const s = Math.pow(-1, Math.floor((k - 1) / 2));
    for (let i = 0; i < phase.length; i++) {
      out[i] += (s * Math.sin(k * phase[i])) / (k * k);
    }
  }
  const scale = 8.0 / (Math.PI * Math.PI);
  for (let i = 0; i < out.length; i++) out[i] *= scale;
  return out;
}

function rampEnv(n, sr, points) {
  const env = new Float64Array(n);
  const times = points.map((p) => p[0]);
  const vals = points.map((p) => p[1]);
  const tEnd = times[times.length - 1];
  for (let i = 0; i < n; i++) {
    const t = i / sr;
    if (t > tEnd) {
      env[i] = 0.0;
      continue;
    }
    // np.interp
    if (t <= times[0]) {
      env[i] = vals[0];
      continue;
    }
    let j = 1;
    while (j < times.length && t > times[j]) j++;
    if (j >= times.length) {
      env[i] = vals[vals.length - 1];
    } else {
      const t0 = times[j - 1], t1 = times[j];
      const v0 = vals[j - 1], v1 = vals[j];
      const u = (t - t0) / (t1 - t0);
      env[i] = v0 + u * (v1 - v0);
    }
  }
  return env;
}

function formantChain(src, sr, f1, f2, f3, gain = 1.0) {
  const specs = [
    [f1, 8.0, 1.0],
    [f2, 10.0, 0.7],
    [f3, 12.0, 0.5],
  ];
  const out = new Float64Array(src.length);
  for (const [f, q, g] of specs) {
    const band = biquad(src, 'bandpass', f, q, sr);
    const scale = g * gain;
    for (let i = 0; i < out.length; i++) out[i] += band[i] * scale;
  }
  return out;
}

function renderVowels(sr = 44100) {
  const dur = 3.4;
  const n = Math.floor(sr * dur);
  const V = {
    a: [730, 1090, 2440],
    i: [270, 2290, 3010],
    u: [300, 870, 2240],
  };
  const seq = [
    ['a', 0.1],
    ['i', 1.2],
    ['u', 2.3],
  ];
  const out = new Float64Array(n);
  const phase = new Float64Array(n);
  const f0 = 125.0;
  for (let i = 0; i < n; i++) phase[i] = 2 * Math.PI * f0 * (i / sr);
  const sawWave = saw(phase, f0, sr);
  for (const [v, t0] of seq) {
    const env = rampEnv(n, sr, [
      [t0, 0.0],
      [t0 + 0.06, 0.9],
      [t0 + 0.8, 0.9],
      [t0 + 1.0, 0.0],
    ]);
    const src = new Float64Array(n);
    for (let i = 0; i < n; i++) src[i] = sawWave[i] * env[i];
    const [f1, f2, f3] = V[v];
    const formant = formantChain(src, sr, f1, f2, f3);
    for (let i = 0; i < n; i++) out[i] += formant[i];
  }
  for (let i = 0; i < n; i++) out[i] *= 0.9;
  return out;
}

function renderTones(sr = 44100) {
  const dur = 2.9;
  const n = Math.floor(sr * dur);
  const out = new Float64Array(n);
  const sweeps = [
    [0.15, 110.0, 175.0],
    [1.45, 180.0, 105.0],
  ];
  for (const [t0, fa, fb] of sweeps) {
    const phase = new Float64Array(n);
    let cum = 0.0;
    for (let i = 0; i < n; i++) {
      const t = i / sr;
      let f;
      if (t < t0) f = fa;
      else if (t > t0 + 0.7) f = fb;
      else f = fa + ((t - t0) / 0.7) * (fb - fa);
      cum += f;
      phase[i] = (2 * Math.PI * cum) / sr;
    }
    const sawWave = saw(phase, Math.max(fa, fb), sr);
    const env = rampEnv(n, sr, [
      [t0, 0.0],
      [t0 + 0.06, 0.9],
      [t0 + 0.6, 0.9],
      [t0 + 0.8, 0.0],
    ]);
    const src = new Float64Array(n);
    for (let i = 0; i < n; i++) src[i] = sawWave[i] * env[i];
    const formant = formantChain(src, sr, 730, 1090, 2440);
    for (let i = 0; i < n; i++) out[i] += formant[i];
  }
  for (let i = 0; i < n; i++) out[i] *= 0.9;
  return out;
}

function renderMelody(sr = 44100) {
  const step = 0.3;
  const notes = [262, 262, 392, 392, 440, 440, 392, 349, 349, 330, 330, 294, 294, 262];
  const dur = 4.5;
  const n = Math.floor(sr * dur);
  const out = new Float64Array(n);
  for (let ni = 0; ni < notes.length; ni++) {
    const f = notes[ni];
    const t0 = ni * step;
    const i0 = Math.floor(t0 * sr);
    const i1 = Math.min(n, Math.floor((t0 + step) * sr));
    if (i1 <= i0) continue;
    const m = i1 - i0;
    const phase = new Float64Array(m);
    for (let i = 0; i < m; i++) phase[i] = 2 * Math.PI * f * (i / sr);
    const triWave = tri(phase, f, sr);
    const a = Math.max(1, Math.floor(0.02 * sr));
    const env = new Float64Array(m);
    // np.linspace(1e-4, 0.9, a)
    if (a === 1) {
      env[0] = 0.9;
    } else {
      for (let i = 0; i < a; i++) env[i] = 1e-4 + (0.9 - 1e-4) * (i / (a - 1));
    }
    const rest = m - a;
    if (rest > 0) {
      const denom = Math.max(rest, 1);
      for (let i = 0; i < rest; i++) {
        env[a + i] = 0.9 * Math.pow(0.08 / 0.9, i / denom);
      }
    }
    for (let i = 0; i < m; i++) {
      const sine = Math.sin(2 * Math.PI * (f * 2) * (i / sr)) * 0.25;
      out[i0 + i] += (triWave[i] + sine) * env[i];
    }
  }
  for (let i = 0; i < n; i++) out[i] *= 0.6;
  return out;
}

const SAMPLES = {
  vowel: renderVowels,
  tone: renderTones,
  melody: renderMelody,
};

// =====================================================================
// 7. WAV 读写（Node；纯 PCM）
// =====================================================================

function readWav(filePath) {
  const buf = fs.readFileSync(filePath);
  if (buf.toString('ascii', 0, 4) !== 'RIFF' || buf.toString('ascii', 8, 12) !== 'WAVE') {
    throw new Error('不是有效的 WAV 文件');
  }
  let offset = 12;
  let sr = 44100, nch = 1, width = 2, dataOffset = -1, dataSize = 0;
  while (offset + 8 <= buf.length) {
    const id = buf.toString('ascii', offset, offset + 4);
    const size = buf.readUInt32LE(offset + 4);
    const start = offset + 8;
    if (id === 'fmt ') {
      const format = buf.readUInt16LE(start);
      if (format !== 1) throw new Error('仅支持 PCM WAV；请先转换格式。');
      nch = buf.readUInt16LE(start + 2);
      sr = buf.readUInt32LE(start + 4);
      width = buf.readUInt16LE(start + 14) / 8;
    } else if (id === 'data') {
      dataOffset = start;
      dataSize = size;
      break;
    }
    offset = start + size + (size & 1);
  }
  if (dataOffset < 0) throw new Error('WAV 缺少 data chunk');
  if (![1, 2, 4].includes(width)) {
    throw new Error('仅支持 8/16/32 位 PCM WAV；请先转换格式。');
  }
  const nFrames = Math.floor(dataSize / (width * nch));
  const mono = new Float64Array(nFrames);
  for (let i = 0; i < nFrames; i++) {
    let sum = 0;
    for (let c = 0; c < nch; c++) {
      const o = dataOffset + (i * nch + c) * width;
      let sample;
      if (width === 1) {
        sample = (buf.readUInt8(o) - 128) / 128.0;
      } else if (width === 2) {
        sample = buf.readInt16LE(o) / 32767.0;
      } else {
        sample = buf.readInt32LE(o) / 2147483647.0;
      }
      sum += sample;
    }
    mono[i] = sum / nch;
  }
  return { data: mono, sr };
}

function writeWav(filePath, x, sr) {
  const n = x.length;
  const buffer = Buffer.alloc(44 + n * 2);
  buffer.write('RIFF', 0);
  buffer.writeUInt32LE(36 + n * 2, 4);
  buffer.write('WAVE', 8);
  buffer.write('fmt ', 12);
  buffer.writeUInt32LE(16, 16);
  buffer.writeUInt16LE(1, 20);
  buffer.writeUInt16LE(1, 22);
  buffer.writeUInt32LE(sr, 24);
  buffer.writeUInt32LE(sr * 2, 28);
  buffer.writeUInt16LE(2, 32);
  buffer.writeUInt16LE(16, 34);
  buffer.write('data', 36);
  buffer.writeUInt32LE(n * 2, 40);
  for (let i = 0; i < n; i++) {
    let v = x[i];
    if (v > 1) v = 1;
    else if (v < -1) v = -1;
    // 与 numpy astype(np.int16) 一致：向零截断，不用 round
    const pcm = (v < 0 ? v * 0x8000 : v * 0x7fff) | 0;
    buffer.writeInt16LE(pcm, 44 + i * 2);
  }
  fs.writeFileSync(filePath, buffer);
}

// =====================================================================
// 8. 可懂度估分
// =====================================================================

function intelligibility(cfg) {
  const eff = cfg.nChannels * (1 - 0.5 * cfg.spread);
  const specShow = clamp(1 - Math.pow(0.72, eff), 0, 1);
  const spec = Math.pow(specShow, 1.6);
  let pitch = clamp(Math.sqrt(Math.max(0.0, cfg.envCut - 20) / 480), 0, 1);
  if (cfg.carrier === 'sine') pitch = clamp(pitch + 0.12, 0, 1);
  const noiseMargin = clamp(1 - cfg.noiseLevel * 1.05, 0, 1);
  const cover = clamp(
    (Math.log(cfg.fHi) - Math.log(cfg.fLo)) / (Math.log(8000) - Math.log(80)),
    0.4,
    1
  );
  const core = clamp(spec * 0.9 + pitch * 0.08, 0, 1);
  const s01 = clamp(core * (0.3 + 0.7 * noiseMargin) * (0.78 + 0.22 * cover), 0, 1);
  const score = Math.round(s01 * 100);
  let grade;
  if (score < 24) grade = '几乎听不懂';
  else if (score < 44) grade = '很吃力';
  else if (score < 66) grade = '大致能懂';
  else if (score < 86) grade = '比较清楚';
  else grade = '接近清晰';
  return { score, grade, spectral: specShow, pitch, noiseMargin };
}

// =====================================================================
// 9. 命令行
// =====================================================================

function parseArgs(argv) {
  const a = {
    input: null,
    sample: null,
    output: 'cochlear_out.wav',
    scenario: null,
    channels: null,
    carrier: null,
    lo: null,
    hi: null,
    envCut: null,
    spread: null,
    noise: null,
    noCompress: false,
    noNormalize: false,
    seed: null,
    listScenarios: false,
  };
  for (let i = 0; i < argv.length; i++) {
    const k = argv[i];
    const next = () => {
      i++;
      if (i >= argv.length) throw new Error(`缺少参数值: ${k}`);
      return argv[i];
    };
    if (k === '-i' || k === '--input') a.input = next();
    else if (k === '-s' || k === '--sample') a.sample = next();
    else if (k === '-o' || k === '--output') a.output = next();
    else if (k === '--scenario') a.scenario = next();
    else if (k === '-n' || k === '--channels') a.channels = +next();
    else if (k === '--carrier') a.carrier = next();
    else if (k === '--lo') a.lo = +next();
    else if (k === '--hi') a.hi = +next();
    else if (k === '--env-cut') a.envCut = +next();
    else if (k === '--spread') a.spread = +next();
    else if (k === '--noise') a.noise = +next();
    else if (k === '--no-compress') a.noCompress = true;
    else if (k === '--no-normalize') a.noNormalize = true;
    else if (k === '--seed') a.seed = +next();
    else if (k === '--list-scenarios') a.listScenarios = true;
    else if (k === '-h' || k === '--help') {
      console.log(`人工耳蜗声码器模拟（Python 版的百分百 JS 还原）

用法:
  node cochlear_vocoder.js [--sample vowel|tone|melody] [-i in.wav] [-o out.wav]
       [--scenario quiet|restaurant|phone|music|tone|minimal]
       [-n channels] [--carrier noise|sine] [--lo Hz] [--hi Hz]
       [--env-cut Hz] [--spread 0-100] [--noise 0-100]
       [--no-compress] [--no-normalize] [--seed N] [--list-scenarios]`);
      process.exit(0);
    } else {
      throw new Error(`未知参数: ${k}`);
    }
  }
  if (a.sample && !SAMPLES[a.sample]) throw new Error(`未知 sample: ${a.sample}`);
  if (a.scenario && !SCENARIOS[a.scenario]) throw new Error(`未知 scenario: ${a.scenario}`);
  if (a.carrier && a.carrier !== 'noise' && a.carrier !== 'sine') {
    throw new Error(`未知 carrier: ${a.carrier}`);
  }
  return a;
}

function main(argv) {
  const a = parseArgs(argv || process.argv.slice(2));

  if (a.listScenarios) {
    for (const [k, v] of Object.entries(SCENARIOS)) {
      console.log(`${k.padEnd(11)} ${JSON.stringify(v)}`);
    }
    return 0;
  }

  const cfg = VocoderConfig();
  let sampleName = a.sample || 'vowel';

  if (a.scenario) {
    const s = { ...SCENARIOS[a.scenario] };
    sampleName = a.sample || s.sample;
    delete s.sample;
    Object.assign(cfg, s);
  }

  if (a.channels != null) cfg.nChannels = Math.floor(clamp(a.channels, 1, 32));
  if (a.carrier) cfg.carrier = a.carrier;
  if (a.lo != null) cfg.fLo = a.lo;
  if (a.hi != null) cfg.fHi = a.hi;
  if (a.envCut != null) cfg.envCut = clamp(a.envCut, 20, 500);
  if (a.spread != null) cfg.spread = clamp(a.spread / 100.0, 0, 1);
  if (a.noise != null) cfg.noiseLevel = clamp(a.noise / 100.0, 0, 1);
  cfg.compress = !a.noCompress;
  cfg.normalize = a.noNormalize ? null : 0.89;
  cfg.seed = a.seed;

  let x, sr, srcTag;
  if (a.input) {
    ({ data: x, sr } = readWav(a.input));
    srcTag = 'file';
  } else {
    sr = 44100;
    x = SAMPLES[sampleName](sr);
    srcTag = sampleName;
    const origPath = a.output.replace(/\.wav$/i, '_original.wav');
    writeWav(origPath, x, sr);
    console.log(`原始参考音已保存: ${origPath}`);
  }

  const y = vocode(x, sr, cfg);
  writeWav(a.output, y, sr);

  const ii = intelligibility(cfg);
  console.log(`已输出: ${a.output}`);
  console.log(`  来源: ${srcTag} · ${(x.length / sr).toFixed(2)}s @ ${sr} Hz`);
  console.log(
    `  通道: ${cfg.nChannels} · 载体: ${cfg.carrier} · ${cfg.fLo.toFixed(0)}–${cfg.fHi.toFixed(0)} Hz`
  );
  console.log(
    `  包络截止: ${cfg.envCut.toFixed(0)} Hz · 扩散: ${(cfg.spread * 100).toFixed(0)}% · 噪声: ${(cfg.noiseLevel * 100).toFixed(0)}%`
  );
  console.log(`  可懂度估分: ${ii.score}/100 (${ii.grade})`);
  return 0;
}

module.exports = {
  biquadCoeffs,
  biquad,
  lfilter,
  clamp,
  logEdges,
  VocoderConfig,
  SCENARIOS,
  compressor,
  makeBabbleNoise,
  vocode,
  renderVowels,
  renderTones,
  renderMelody,
  SAMPLES,
  readWav,
  writeWav,
  intelligibility,
  main,
};

if (require.main === module) {
  try {
    process.exit(main());
  } catch (err) {
    console.error(err.message || err);
    process.exit(1);
  }
}
