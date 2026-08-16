# Proguard rules for AIRI Phase 1.
# Keep JNI classes — llama.cpp bridge methods are called by name from C++.
-keep class com.airi.odslm.jni.** { *; }

# Room generated code
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**
