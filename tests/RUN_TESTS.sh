#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p /tmp/pandora-parser-test
javac -encoding UTF-8 -d /tmp/pandora-parser-test app/src/main/java/com/jarvis/kisisel/MemoryCommandParser.java tests/MemoryCommandParserTest.java
java -cp /tmp/pandora-parser-test MemoryCommandParserTest

javac -encoding UTF-8 -d /tmp/pandora-parser-test app/src/main/java/com/jarvis/kisisel/ai/FreeCloudPolicy.java tests/FreeCloudPolicyTest.java
java -cp /tmp/pandora-parser-test FreeCloudPolicyTest

# Standalone greeting classifier is tested outside Android.
javac -encoding UTF-8 -d /tmp/pandora-parser-test app/src/main/java/com/jarvis/kisisel/ConversationIntent.java tests/ConversationIntentTest.java
java -cp /tmp/pandora-parser-test ConversationIntentTest

# Wake word recognition classifier
javac -encoding UTF-8 -d /tmp/pandora-parser-test app/src/main/java/com/jarvis/kisisel/WakeWordMatcher.java tests/WakeWordMatcherTest.java
java -cp /tmp/pandora-parser-test WakeWordMatcherTest

# Wake words with limited read-only tasks.
javac -encoding UTF-8 -d /tmp/pandora-parser-test app/src/main/java/com/jarvis/kisisel/WakeWordMatcher.java app/src/main/java/com/jarvis/kisisel/WakeQuickCommand.java tests/WakeQuickCommandTest.java
java -cp /tmp/pandora-parser-test WakeQuickCommandTest

# Direct wake-command replies remain headphone-only, and are suppressed during calls.
javac -encoding UTF-8 -d /tmp/pandora-parser-test app/src/main/java/com/jarvis/kisisel/WakeVoicePolicy.java tests/WakeVoicePolicyTest.java
java -cp /tmp/pandora-parser-test WakeVoicePolicyTest

# Shared text, URLs, and data size guard.
javac -encoding UTF-8 -d /tmp/pandora-parser-test app/src/main/java/com/jarvis/kisisel/SharedTextGuard.java tests/SharedTextGuardTest.java
java -cp /tmp/pandora-parser-test SharedTextGuardTest

# Screen reading is local by default, separate explicit consent before cloud analysis.
javac -encoding UTF-8 -d /tmp/pandora-parser-test app/src/main/java/com/jarvis/kisisel/ScreenCloudPolicy.java tests/ScreenCloudPolicyTest.java
java -cp /tmp/pandora-parser-test ScreenCloudPolicyTest

# Wake-only prompts can be followed by a command without the wake word.
javac -encoding UTF-8 -d /tmp/pandora-parser-test app/src/main/java/com/jarvis/kisisel/WakeWordMatcher.java app/src/main/java/com/jarvis/kisisel/WakeQuickCommand.java app/src/main/java/com/jarvis/kisisel/WakeConversationState.java tests/WakeConversationStateTest.java
java -cp /tmp/pandora-parser-test WakeConversationStateTest

# Quick Settings tile requires Accessibility permission, not a microphone service.
javac -encoding UTF-8 -d /tmp/pandora-parser-test app/src/main/java/com/jarvis/kisisel/PandoraTilePolicy.java tests/PandoraTilePolicyTest.java
java -cp /tmp/pandora-parser-test PandoraTilePolicyTest

# Background screen reviews and spoken Q&A never advertise device actions to the AI.
javac -encoding UTF-8 -d /tmp/pandora-parser-test app/src/main/java/com/jarvis/kisisel/ai/ModelIntentPolicy.java tests/ModelIntentPolicyTest.java
java -cp /tmp/pandora-parser-test ModelIntentPolicyTest

# Never capture password input screens, own app, or notification shade.
javac -encoding UTF-8 -d /tmp/pandora-parser-test app/src/main/java/com/jarvis/kisisel/ScreenSourcePolicy.java tests/ScreenSourcePolicyTest.java
java -cp /tmp/pandora-parser-test ScreenSourcePolicyTest

# WhatsApp: don't mistake chat text or an active video call for a safe switch button.
javac -encoding UTF-8 -d /tmp/pandora-parser-test app/src/main/java/com/jarvis/kisisel/WhatsAppCallPolicy.java tests/WhatsAppCallPolicyTest.java
java -cp /tmp/pandora-parser-test WhatsAppCallPolicyTest

# Screen analysis cannot use stale content from another app or a password screen.
javac -encoding UTF-8 -d /tmp/pandora-parser-test \
  app/src/main/java/com/jarvis/kisisel/ScreenSourcePolicy.java \
  app/src/main/java/com/jarvis/kisisel/ScreenCapturePolicy.java \
  tests/ScreenCapturePolicyTest.java
java -cp /tmp/pandora-parser-test ScreenCapturePolicyTest

# Wake phrase requires complete speech, plausible duration and confidence.
javac -encoding UTF-8 -d /tmp/pandora-parser-test app/src/main/java/com/jarvis/kisisel/WakePhraseGate.java tests/WakePhraseGateTest.java
java -cp /tmp/pandora-parser-test WakePhraseGateTest

# Explicit user-requested Hermes task routing must not misclassify ordinary questions.
javac -encoding UTF-8 -d /tmp/pandora-parser-test app/src/main/java/com/jarvis/kisisel/ai/HybridRoutePolicy.java tests/HybridRoutePolicyTest.java
java -cp /tmp/pandora-parser-test HybridRoutePolicyTest
