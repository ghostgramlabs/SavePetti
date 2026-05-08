# ── Android / Kotlin baseline ────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keepattributes SourceFile, LineNumberTable

# ── Room ─────────────────────────────────────────────────────────────────
# Concrete database name (avoids "class * extends RoomDatabase" rule which
# triggers an R8 ConcurrentModificationException in 8.5.x with this graph).
-keep class com.savepetti.data.local.AppDatabase { *; }
-keep class com.savepetti.data.local.AppDatabase_Impl { *; }
-keep class com.savepetti.data.local.** { *; }
-keepclassmembers class com.savepetti.data.local.** {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}
-dontwarn androidx.room.paging.**

# ── Hilt / Dagger ────────────────────────────────────────────────────────
# Hilt generates names by convention; preserve the structure but allow R8 to
# inline / shrink unused parts.
-keep class hilt_aggregated_deps.** { *; }
-keep class **_HiltModules { *; }
-keep class **_HiltModules$* { *; }
-keep class **_GeneratedInjector { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.AndroidEntryPoint <init>(...);
}
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}

# ── ML Kit text recognition (loads model classes reflectively) ───────────
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-dontwarn com.google.mlkit.**

# ── Coil ─────────────────────────────────────────────────────────────────
-dontwarn coil.**

# ── Jsoup ────────────────────────────────────────────────────────────────
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# ── Kotlin coroutines ────────────────────────────────────────────────────
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**
-dontwarn kotlinx.coroutines.flow.**
