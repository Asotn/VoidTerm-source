# VoidTerm ProGuard rules
# Developer: Asotn

# Keep the JNI native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep all classes in the package
-keep class com.asotn.voidterm.** { *; }

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Androidx / Material
-keep class androidx.** { *; }
-dontwarn androidx.**

# Suppress warnings for known safe removes
-dontwarn java.lang.invoke.**
-dontwarn **$$Lambda$*
