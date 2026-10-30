# kotlinx.serialization ships its own rules; keep our @Serializable models' generated serializers.
-keepclassmembers @kotlinx.serialization.Serializable class com.medassist.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.medassist.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Ktor / OkHttp reference optional JVM classes that do not exist on Android.
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
