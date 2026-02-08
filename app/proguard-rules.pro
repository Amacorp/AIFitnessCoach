# ProGuard rules for AI Fitness Coach

# Keep MediaPipe classes
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# Keep Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep Gson
-keep class com.google.gson.** { *; }
-keep class com.aicoach.fitness.data.remote.** { *; }

# Keep model classes
-keep class com.aicoach.fitness.domain.model.** { *; }
