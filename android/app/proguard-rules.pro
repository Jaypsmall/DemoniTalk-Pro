# Ignore missing Play Core references from Flutter engine
-dontwarn com.google.android.play.core.**
-dontwarn io.flutter.embedding.engine.deferredcomponents.**

# Flutter ProGuard Rules
-keep class io.flutter.app.** { *; }
-keep class io.flutter.plugin.** { *; }
-keep class io.flutter.util.** { *; }
-keep class io.flutter.view.** { *; }
-keep class io.flutter.embedding.** { *; }

# Keep Vosk Speech Recognition Engine classes and native JNI methods
-keep class org.vosk.** { *; }
-keepclassmembers class org.vosk.** { *; }

# Keep Native Engine Rust JNI bindings
-keep class com.jaylizapp.demonitalk_pro.NativeEngine { *; }
-keepclassmembers class com.jaylizapp.demonitalk_pro.NativeEngine {
    native <methods>;
    *;
}

# Keep all App Kotlin classes from obfuscation
-keep class com.jaylizapp.demonitalk_pro.** { *; }
-keepclassmembers class com.jaylizapp.demonitalk_pro.** { *; }

# Keep Gson models and serialization
-keep class com.google.gson.** { *; }
-keepclassmembers class com.google.gson.** { *; }

# Keep Android Speech API
-keep class android.speech.** { *; }

-keep class com.google.android.play.core.** { *; }

# Reglas indispensables para JNA (usado por Vosk) en modo Release
-keep class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**

# Asegurar que las estructuras de JNA no se modifiquen
-keepclassmembers class * extends com.sun.jna.Structure {
    protected void *();
    public void *();
    *;
}