"""Fetch the pinned Apache-2.0 Turkish speech model; no runtime download needed."""
import hashlib, pathlib, urllib.request, zipfile, tempfile
ROOT = pathlib.Path(__file__).resolve().parents[1]
DEST = ROOT / "app/src/main/assets/model-tr"
DIGEST = "8c8d07cec1bce31add14967c16891c84152cdf76d391e33c08278119b9ea96e5"
if not (DEST / "uuid").exists():
    with tempfile.TemporaryDirectory() as folder:
        archive = pathlib.Path(folder) / "model.zip"
        urllib.request.urlretrieve("https://alphacephei.com/vosk/models/vosk-model-small-tr-0.3.zip", archive)
        if hashlib.sha256(archive.read_bytes()).hexdigest() != DIGEST:
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
        (DEST / "uuid").write_text(DIGEST)
print("Pinned Turkish speech model ready")
