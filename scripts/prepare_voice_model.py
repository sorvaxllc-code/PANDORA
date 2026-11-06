"""Fetch the pinned Apache-2.0 Turkish speech model; no runtime download needed."""
import hashlib, pathlib, urllib.request, zipfile, tempfile
ROOT = pathlib.Path(__file__).resolve().parents[1]
DEST = ROOT / "app/src/main/assets/model-tr"
DIGEST = "8c8d07cec1bce31add14967c16891c84152cdf76d391e33c08278119b9ea96e5"
MODELS = [("model-tr", "vosk-model-small-tr-0.3", DIGEST),
          ("model-wake", "vosk-model-small-en-us-0.15", "30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498")]
for asset, name, digest in MODELS:
    DEST = ROOT / "app/src/main/assets" / asset
    if (DEST / "uuid").exists(): continue
    with tempfile.TemporaryDirectory() as folder:
        archive = pathlib.Path(folder) / "model.zip"
        urllib.request.urlretrieve(f"https://alphacephei.com/vosk/models/{name}.zip", archive)
        if hashlib.sha256(archive.read_bytes()).hexdigest() != digest:
            raise RuntimeError("Speech model checksum mismatch")
        with zipfile.ZipFile(archive) as z:
            for item in z.infolist():
                path = pathlib.PurePosixPath(item.filename)
                if ".." in path.parts or path.is_absolute():
                    raise RuntimeError("Unsafe archive path")
                if item.is_dir(): continue
                target = DEST.joinpath(*path.parts[1:])
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(z.read(item))
        (DEST / "uuid").write_text(digest)
print("Pinned Turkish speech model ready")
