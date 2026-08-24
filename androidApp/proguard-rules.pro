# kotlinx.serialization: keep generated serializers and @Serializable metadata.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.joelkanyi.platypus.**$$serializer { *; }
-keepclassmembers class com.joelkanyi.platypus.** {
    *** Companion;
}
-keepclasseswithmembers class com.joelkanyi.platypus.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor client (uses reflection for engines/plugins on some paths).
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-dontwarn io.ktor.**
-dontwarn org.slf4j.**
-dontwarn kotlinx.coroutines.**

# Room: database, DAOs and entities.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**

# Coil 3.
-dontwarn coil3.**

# Tink (via androidx.security.crypto EncryptedSharedPreferences) references build-time-only annotations.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.crypto.tink.**

# Kotlin metadata for reflection-lite used by serialization.
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
