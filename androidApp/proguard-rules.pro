# kotlinx.serialization: conserva los serializadores generados de los DTO.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class pe.pluscontable.cliente.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class pe.pluscontable.cliente.**$$serializer { *; }

# Ktor / OkHttp: avisos de clases opcionales que no se usan en Android.
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Tink (security-crypto) referencia anotaciones de errorprone que no van en el APK.
-dontwarn com.google.errorprone.annotations.**
