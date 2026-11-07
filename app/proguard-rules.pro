# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class dev.mouya.phiraforge.**$$serializer { *; }
-keepclassmembers class dev.mouya.phiraforge.** {
    *** Companion;
}
-keepclasseswithmembers class dev.mouya.phiraforge.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Miuix
-keep class top.yukonga.miuix.kmp.** { *; }
-dontwarn top.yukonga.miuix.kmp.**