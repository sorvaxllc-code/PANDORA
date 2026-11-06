# Speech recognition

Vosk Android 0.3.75 and vosk-model-small-tr-0.3: Alpha Cephei, Apache License 2.0.
Sources: https://github.com/alphacep/vosk-api and https://alphacephei.com/vosk/models
The unmodified model is bundled during CI using a pinned SHA-256.
JNA 5.18.1: https://github.com/java-native-access/jna (LGPL 2.1 or Apache 2.0).

Voice accuracy, including the wake phrase, must be checked on device.

Wake vocabulary: vosk-model-small-en-us-0.15 (Apache 2.0). Turkish command vocabulary does not contain Pandora; a separate wake recognizer is required.
