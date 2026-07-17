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

# 保留调试堆栈的行号与源文件名
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 保留泛型签名（Gson/Moshi 反射需要 TypeToken 的 ParameterizedType）
-keepattributes Signature
# 保留注解（@SerializedName / @Json / @SuppressLint 等）
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations

# 保留所有带 @SerializedName 注解的字段
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 保留数据模型类本身（字段名 + 默认构造），防止 Gson UnsafeAllocator 绕过构造时字段丢失
-keep class me.neko.nzhelper.core.model.** { *; }

# 保留 Gson TypeAdapter / JsonSerializer / JsonDeserializer 实现类
-keep class * implements com.google.gson.TypeAdapter { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }

# Gson 反射需要保留无参构造（数据类没有时，至少保留 $default 反射入口）
-keepclasseswithmembers,allowobfuscation class * {
    <init>();
}

# 保留 Moshi codegen / reflect 生成的 JsonAdapter 类
-keep class *JsonAdapter { <init>(...); *; }
-keep class me.neko.nzhelper.ui.util.*JsonAdapter { <init>(...); *; }

# 保留 Moshi Kotlin 反射工厂
-keep class com.squareup.moshi.kotlin.reflect.** { *; }
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# GitHubRelease 数据类（@Json 注解字段）
-keep class me.neko.nzhelper.core.util.GitHubRelease { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# UpdateChecker 单例
-keep class me.neko.nzhelper.core.util.UpdateChecker { *; }

-dontwarn okhttp3.**
-dontwarn okio.**

-keep class okhttp3.OkHttpClient { *; }
-keep class okhttp3.Request { *; }
-keep class okhttp3.Request$Builder { *; }
-keep class okhttp3.Response { *; }
-keep class okhttp3.ResponseBody { *; }
-keep class okhttp3.Call { *; }
-keepclassmembers class okhttp3.Call {
    public okhttp3.Response execute();
}

-keep class okio.** { *; }

-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

-keep class me.neko.nzhelper.MainActivity { *; }
-keep class me.neko.nzhelper.NzApplication { *; }
-keep class me.neko.nzhelper.core.service.TimerService { *; }
-keep class me.neko.nzhelper.core.service.TimerService$* { *; }
-keep class me.neko.nzhelper.feature.**Activity { *; }
-keep class me.neko.nzhelper.feature.**Activity$* { *; }

# WorkManager
-keep class me.neko.nzhelper.core.worker.** { *; }
-dontwarn androidx.work.**

# Room
-keep class me.neko.nzhelper.core.database.entity.** { *; }
-keep class me.neko.nzhelper.core.database.AppDatabase { *; }
-keep class me.neko.nzhelper.core.database.AppDatabase$* { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}
-dontwarn androidx.room.paging.**

# SQLCipher
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.** { *; }
-dontwarn net.zetetic.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**