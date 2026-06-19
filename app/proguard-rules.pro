# ONNX Runtime talks to its native libs over JNI, which looks up these Java
# classes, fields, and methods by name. Shrinking/renaming them breaks OCR.
-keep class ai.onnxruntime.** { *; }
-keepclassmembers class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# WorkManager instantiates these workers by class name via reflection.
-keep class com.astrove.work.** { *; }
