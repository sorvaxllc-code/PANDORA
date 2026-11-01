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

# Direct wake-command replies are audible without headphones, unless user disables speech or is in a call.
javac -encoding UTF-8 -d /tmp/pandora-parser-test app/src/main/java/com/jarvis/kisisel/WakeVoicePolicy.java tests/WakeVoicePolicyTest.java
java -cp /tmp/pandora-parser-test WakeVoicePolicyTest
