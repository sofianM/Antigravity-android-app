# Keep kotlinx.serialization models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.sofianm.antigravity.data.**$$serializer { *; }
-keepclassmembers class com.sofianm.antigravity.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.sofianm.antigravity.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep WebView JS bridge methods
-keepclassmembers class com.sofianm.antigravity.web.WebNotificationBridge {
    @android.webkit.JavascriptInterface <methods>;
}
