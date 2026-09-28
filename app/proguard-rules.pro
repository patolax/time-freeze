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
-keepattributes SourceFile,LineNumberTable
-keep class com.time.freezer.filters.*
{ *; }

# R8 full mode strips the no-arg constructor off Room's generated database classes since it can't
# see that Room/WorkManager instantiate them reflectively - crashes every release build on launch
# with "Failed to create an instance of androidx.work.impl.WorkDatabase" otherwise.
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile