# KeuMusic AI — OpenJarvis adaptation notes

This document records the architecture patterns evaluated from the OpenJarvis project.

## Compatibility decision

OpenJarvis is a Python-first personal AI framework. KeuMusic is a native Android application written in Kotlin and Jetpack Compose. The Python runtime, desktop-oriented inference engines, and container-based agent execution are therefore not embedded in the APK.

Instead, KeuMusic adopts compatible design patterns in native Android code:

- **Explicit local memory:** user-authored facts are stored in app-private preferences, only while the memory switch is enabled.
- **Tool-oriented command handling:** local music actions remain controlled by the Android player callbacks rather than allowing a language model to execute arbitrary code.
- **Privacy boundary:** saved long-term facts are not automatically sent to a cloud model.
- **Incremental architecture:** future capabilities can be added as isolated tools with clear inputs, outputs, and permission requirements.

## Current long-term memory commands

When local memory is enabled, the user can say:

- "Lembre que ..." — save a fact on this device.
- "O que você lembra?" — retrieve saved facts.
- "Esqueça que ..." — remove a matching fact.
- "Apague minhas memórias" — clear long-term facts.

Disabling the memory switch clears both the conversation history and saved long-term facts.

## Attribution and licensing

OpenJarvis is an independent upstream project published under Apache License 2.0. This adaptation note records architectural inspiration; the KeuMusic implementation described above is native Kotlin code and does not vendor the OpenJarvis Python runtime.
