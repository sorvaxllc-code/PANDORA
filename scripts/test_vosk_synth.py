#!/usr/bin/env python3
"""Acoustic smoke test of the exact bundled English wake model, using synthetic speech.

Synthetic TTS is not a substitute for Turkish-accent / Bluetooth / factory tests.
Tests only check that a known utterance can be decoded, and publish confidence
so we can tune final-only gating without pretending a device test happened.
"""
import json
import math
import random
import shutil
import struct
import subprocess
import tempfile
from pathlib import Path
import wave

from vosk import Model, KaldiRecognizer, SetLogLevel

SetLogLevel(-1)
root = Path(__file__).resolve().parent.parent
model = Model(str(root / "app/src/main/assets/model-wake"))
grammar = '["hey pandora", "hi pandora", "[unk]"]'

def score(pcm: bytes, label: str) -> dict:
    recognizer = KaldiRecognizer(model, 16000, grammar)
    recognizer.SetWords(True)
    final = {}
    voiced_ms = 0
    for offset in range(0, len(pcm), 3200):
        chunk = pcm[offset:offset + 3200]
        if len(chunk) < 2: continue
        samples = struct.unpack("<" + "h" * (len(chunk) // 2), chunk)
        rms = math.sqrt(sum(v * v for v in samples) / len(samples))
        if rms > 420: voiced_ms += len(samples) * 1000 // 16000
        if recognizer.AcceptWaveform(chunk):
            final = json.loads(recognizer.Result())
    if not final.get("text"):
        final = json.loads(recognizer.FinalResult())
    words = final.get("result", [])
    confidence = sum(w.get("conf", 0) for w in words) / len(words) if words else -1
    verified = (final.get("text") in ("hey pandora", "hi pandora")
                and 350 <= voiced_ms <= 5500
                and (confidence >= .58 or (confidence < 0 and voiced_ms >= 680)))
    result = {
        "test":label, "transcript":final.get("text",""),
        "confidence":round(confidence, 3),
        "voiced_ms": voiced_ms,
        "verified":verified,
    }
    print(json.dumps(result))
    return result

# Non-speech test: silence and reproducible white noise.
score(b"\0" * 64000, "2 seconds of silence")
rng = random.Random(24)
data = struct.pack("<" + "h" * 32000,
    *(rng.randint(-3000,3000) for _ in range(32000)))
score(data, "2 seconds of white noise")

voice = shutil.which("espeak-ng") or shutil.which("espeak")
ffmpeg = shutil.which("ffmpeg")
if not voice or not ffmpeg:
    print("Synthetic speech diagnostic skipped: espeak and ffmpeg are not both available")
    raise SystemExit(0)

with tempfile.TemporaryDirectory() as tmp:
    wav = Path(tmp)/"hey.wav"
    pcm_file = Path(tmp)/"hey.pcm"
    subprocess.run([voice, "-v", "en-us", "-s", "125",
        "-w", str(wav), "hey pandora"], check=True)
    subprocess.run([ffmpeg, "-nostdin", "-hide_banner", "-loglevel", "error",
        "-y", "-i", str(wav), "-ar", "16000", "-ac", "1",
        "-f", "s16le", str(pcm_file)], check=True)
    raw = pcm_file.read_bytes() + b"\0" * 32000
    score(raw, "espeak en-us hey pandora")
    # Also print the Turkish eSpeak variant if the voice is installed.
    if "tr" in subprocess.run([voice, "--voices"],
        capture_output=True, text=True).stdout:
        subprocess.run([voice,"-v","tr","-s","125",
            "-w",str(wav),"hey pandora"],check=True)
        subprocess.run([ffmpeg,"-nostdin","-hide_banner","-loglevel","error",
            "-y","-i",str(wav),"-ar","16000","-ac","1",
            "-f","s16le",str(pcm_file)],check=True)
        score(pcm_file.read_bytes() + b"\0"*32000,
            "espeak Turkish voice hey pandora")
