# Hear Like Me · Audio Engine

人工耳蜗声码器离线处理引擎（Python），由 `jsj-mini` 后端通过子进程调用。

## 依赖

- Python 3.9+（仅完整声码器链路需要）
- [FFmpeg](https://ffmpeg.org/)（**系统命令**，不要写入 `requirements.txt`）

### 安装 FFmpeg

FFmpeg 需在系统 PATH 中可用，或于后端配置 `hear-like-me.engine.ffmpeg-path` 指定绝对路径。

```bash
ffmpeg -version
```

Windows 可从 [ffmpeg.org](https://ffmpeg.org/download.html) 下载，解压后将 `bin` 目录加入 PATH。

### 安装 Python 依赖

```bash
cd audio-engine
pip install -r requirements.txt
```

验证 FFmpeg：

```bash
ffmpeg -version
```

## 手动测试

```bash
python cochlear_vocoder.py -i input.wav -o output.wav --channels 8 --carrier noise --lo 150 --hi 7000 --env-cut 160 --spread 15 --noise 0 --scenario quiet
```

说明：`--spread` 与 `--noise` 为 **0–100 的百分比**（与网页 UI 一致）。

## 后端配置

在 `jsj-mini` 的 `application.yml` 中：

```yaml
hear-like-me:
  engine:
    python-path: ${HLM_PYTHON_PATH:python}
    script-path: ${HLM_ENGINE_SCRIPT:../../audio-engine/cochlear_vocoder.py}
    timeout-seconds: 120
    ffmpeg-path: ${HLM_FFMPEG_PATH:ffmpeg}
    vocoder-enabled: ${HLM_VOCODER_ENABLED:true}
```

仅联调 FFmpeg 时可设 `HLM_VOCODER_ENABLED=false`，此时 normalized wav 会直接复制为 output。

从 `scaffolding-v2/jsj-mini` 目录启动时，默认脚本路径为仓库根目录下的 `audio-engine/cochlear_vocoder.py`。

部署到服务器时，建议通过环境变量指定绝对路径：

```bash
export HLM_PYTHON_PATH=/usr/bin/python3
export HLM_ENGINE_SCRIPT=/opt/hear-like-me/audio-engine/cochlear_vocoder.py
export HLM_FFMPEG_PATH=/usr/bin/ffmpeg
```

## 处理流程

1. 用户上传音频 → `audio/input/...`
2. FFmpeg 标准化 → `audio/normalized/.../*.wav`（`AUDIO_NORMALIZED`）
3. Python 声码器 → `audio/output/.../*.wav`（`AUDIO_OUTPUT`）
4. 小程序播放 `processedAudioUrl`
