#!/usr/bin/env python3
"""Print one short-lived Gemini Live auth token. Requires GEMINI_API_KEY env var.
Run locally (e.g., Termux) and paste the auth_tokens/... value into PEGASUS.
Never paste your permanent API key into PEGASUS or a public chat.
"""
import datetime
import json
import os
import sys
import urllib.error
import urllib.request

key = os.environ.get("GEMINI_API_KEY", "").strip()
if not key:
    sys.exit("GEMINI_API_KEY ortam değişkeni eksik. Anahtarı dosyaya koyma.")
now = datetime.datetime.now(datetime.timezone.utc)
iso = lambda t: t.strftime("%Y-%m-%dT%H:%M:%SZ")
request = urllib.request.Request(
    "https://generativelanguage.googleapis.com/v1beta/auth_tokens",
    data=json.dumps({
        "uses": 1,
        "newSessionExpireTime": iso(now + datetime.timedelta(seconds=60)),
        "expireTime": iso(now + datetime.timedelta(minutes=15)),
    }).encode("utf-8"),
    headers={"x-goog-api-key": key, "Content-Type": "application/json"},
    method="POST",
)
try:
    with urllib.request.urlopen(request, timeout=20) as response:
        token = json.loads(response.read().decode("utf-8")).get("name", "")
except urllib.error.HTTPError as exc:
    sys.exit("Gemini token HTTP hatası: " + str(exc.code) + " (API anahtarı/erişimi kontrol et)")
except Exception:
    sys.exit("Gemini token alınamadı. İnternet bağlantısı ve API erişimini kontrol et.")
if not token.startswith("auth_tokens/"):
    sys.exit("API yanıtı beklenen kısa ömürlü anahtarı içermiyor.")
print(token)
