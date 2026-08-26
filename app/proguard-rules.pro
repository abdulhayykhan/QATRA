# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Kotlin Serialization metadata (required by Supabase and app serialization)
-keepattributes *Annotation*
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep @kotlinx.serialization.Serializable class * { *; }

# Supabase - keep serialization metadata only
-keepclassmembers class io.github.jan.supabase.** {
    @kotlinx.serialization.SerialName *;
}
-keep @kotlinx.serialization.Serializable class io.github.jan.supabase.** { *; }

# Keep Supabase plugin service loaders
-keep class io.github.jan.supabase.** extends io.github.jan.supabase.plugins.SupabasePlugin { *; }

# Firebase - keep annotated/serializable classes only
-keep class com.google.firebase.** { *; }

# OkHttp - suppress warnings, keep only essential classes
-dontwarn okhttp3.**
-keep class okhttp3.internal.** { *; }
-keepclassmembers class okhttp3.** {
    @kotlinx.serialization.SerialName *;
}

# Keep data classes used with Supabase
-keep class com.qatra.app.data.model.** { *; }
