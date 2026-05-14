-keep class com.radhanathswami.app.data.** { *; }
-keep class com.radhanathswami.app.service.** { *; }

# Jsoup
-keep public class org.jsoup.** { public *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Media3
-keep class androidx.media3.** { *; }
