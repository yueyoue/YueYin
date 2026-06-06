# ProGuard rules for YueYin

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.yueyin.data.model.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Media3
-keep class androidx.media3.** { *; }

# Keep API client and related classes
-keep class com.yueyin.data.api.** { *; }

# Keep custom converter factory
-keep class * extends retrofit2.Converter$Factory { *; }

# Keep SSL trust-all related classes
-keep class * implements javax.net.ssl.X509TrustManager { *; }
-keep class * implements javax.net.ssl.HostnameVerifier { *; }

# Keep all model classes
-keep class com.yueyin.data.model.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
