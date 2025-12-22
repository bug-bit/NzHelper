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

# === OkHttp3 精确配置 ===
-dontwarn okhttp3.**
-dontwarn okio.**

# 只保留实际使用的OkHttp类
-keep class okhttp3.OkHttpClient { *; }
-keep class okhttp3.Request { *; }
-keep class okhttp3.Request$Builder { *; }
-keep class okhttp3.Response { *; }
-keep class okhttp3.ResponseBody { *; }
-keep class okhttp3.Call { *; }

# 保留execute()方法
-keepclassmembers class okhttp3.Call {
    public okhttp3.Response execute();
}

# 保留所有 Moshi codegen 生成的 JsonAdapter 类
-keep class *JsonAdapter {
    <init>(...);
    *;
}
-keep class me.neko.nzhelper.ui.util.*JsonAdapter {
    <init>(...);
    *;
}
# 保留 data class（可选，通常反射也需要）
-keep class me.neko.nzhelper.ui.util.GitHubRelease {
    *;
}
