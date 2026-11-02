# VitreGram TV Proguard Rules

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Media3 ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Retrofit / Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.vitregram.tv.data.remote.dto.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Compose for TV
-keep class androidx.tv.** { *; }
