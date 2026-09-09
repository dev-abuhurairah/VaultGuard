# VaultGuard ProGuard Rules
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Security Crypto
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
