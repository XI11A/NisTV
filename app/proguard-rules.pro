# ProGuard rules for NisTV
-dontwarn okhttp3.**
-dontwarn okio.**
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}
