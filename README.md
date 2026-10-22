# 😈 DemoniTalk Pro 😈  ![Flutter](https://img.shields.io/badge/Flutter-181717?style=flat&logo=flutter&logoColor=FFD600)
![Kotlin](https://img.shields.io/badge/Kotlin-181717?style=flat&logo=kotlin&logoColor=FFD600)
![Rust](https://img.shields.io/badge/Rust-181717?style=flat&logo=rust&logoColor=FFD600)

¡La versión más potente de **DemoniTalk** reconstruida con **Flutter**, **Kotlin** y **Rust Native Engine**!

---

## 🚀 Arquitectura del Proyecto

```
demoni_pro/
├── lib/                             # Interfaz de Usuario e integración Flutter
│   └── main.dart                    # UI, Control de Ajustes, Importación/Exportación
├── native/
│   └── demoni_engine/               # ⚡ Motor de Reconocimiento y Procesamiento en RUST
│       ├── Cargo.toml               # Dependencias de Rust (regex, unicode-normalization, jni)
│       └── src/
│           ├── lib.rs               # Bindings JNI y C FFI
│           ├── normalizer.rs        # Normalizador ultra-rápido NFD de texto e alias
│           ├── wake_word.rs         # Detector de Wake Word ("demoni", "hermano", "amigo"...)
│           ├── fuzzy.rs             # Algoritmo de fuzzy matching (Levenshtein distance)
│           └── command_processor.rs # Procesador y parser de secuencias de comandos
└── android/                         # Servicios nativos Android en Kotlin
    └── app/src/main/kotlin/com/jaylizapp/demonitalk_pro/
        ├── NativeEngine.kt          # Wrapper JNI para comunicarse con Rust demoni_engine
        ├── VoiceTextNormalizer.kt   # Integración con motor nativo de normalización
        ├── CommandHandler.kt        # Ejecutor de comandos con fallback inteligente
        ├── VoskSpeechManager.kt     # Reconocimiento de voz Offline con Vosk
        └── FloatingButtonService.kt # Botón flotante y servicio de accesibilidad
```

---

## ⚡ Rust Native Engine (`demoni_engine`)

El motor nativo escrito en **Rust** proporciona procesamiento de voz y coincidencia de comandos con latencia inferior a 1 ms:

- **Normalización NFD Novedosa**: Eliminación de acentos, conversión a minúsculas y sustitución dinámica de alias ("demonio", "de money", "de moni", "d money" -> "demoni").
- **Detección de Wake Word**: Reconocimiento inmediato de palabras clave de activación.
- **División de Secuencias Complejas**: Parsea órdenes múltiples en una sola frase (ej. *"Abre WhatsApp y escribe hola luego enviar"*).
- **Fuzzy Matching**: Coincidencia difusa mediante distancia Levenshtein.
- **JNI y C-FFI Integrados**: Exporta métodos `Java_com_jaylizapp_demonitalk_1pro_NativeEngine_*` para Kotlin y símbolos `extern "C"` para Flutter `dart:ffi`.

---

## 📦 Compilación del Motor Rust (Opcional para Android)

Para compilar la librería nativa `.so` usando `cargo ndk`:

```bash
cd native/demoni_engine
cargo build --target aarch64-linux-android --release
```

Y copiar el binario generado (`libdemoni_engine.so`) a:
`android/app/src/main/jniLibs/arm64-v8a/libdemoni_engine.so`

Si la librería nativa no está compilada aún, **DemoniTalk Pro** cuenta con un **Fallback Inteligente a Kotlin** para garantizar que todas las funciones sigan operando perfectamente sin interrupción.

---

## 🔥 Características Principales

1. **Modo Demoníaco Híbrido**: Combina capacidades Root (Shell commands) y Accesibilidad de Android.
2. **Modos de Activación**:
   - 🟡 **Amarillo**: Escucha de orden única.
   - 🔵 **Azul (C4)**: Modo Vigilancia (Wake word).
   - 🟢 **Verde**: Escucha continua.
   - 🔴 **Rojo**: Detención total.
3. **Cerebro e Inteligencia Artificial**: Integración con asistente IA con memoria persistente.
4. **Respaldos e Importación/Exportación**: Importa y exporta la configuración de tus comandos pactados en formato JSON.
