# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep ActiQuest domain models and Firebase entities from obfuscation / field renaming
-keep class com.actiquest.domain.** { *; }
-keepclassmembers class com.actiquest.domain.** { *; }
-keepclassmembers class * extends com.actiquest.domain.** { *; }

-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn com.google.firebase.**
