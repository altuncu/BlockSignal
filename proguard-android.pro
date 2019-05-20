-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn sun.reflect.**
-dontwarn sun.misc.**

-keep class com.eclipsesource.v8.** { *; }
-keep class org.blockstack.android.sdk.** { *; }
-keep public class com.google.android.gms.* { public *; }
-dontwarn com.google.android.gms.**

# ServiceLoader support
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Most of volatile fields are updated with AFU and should not be mangled
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}