# =============================================================================
# Vocalize – ProGuard / R8 Rules
# =============================================================================

# ── Kotlin / Coroutines ──────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-keepattributes SourceFile, LineNumberTable
-keepattributes Signature, Exceptions
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keep class kotlin.coroutines.** { *; }
-keepclassmembers class **$WhenMappings { *; }
-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }

# ── Room Database ────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Dao class * { *; }
-dontwarn androidx.room.**

# ── Hilt (Dagger) ────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclassmembers class * { @javax.inject.Inject <init>(...); }
-dontwarn dagger.**

# ── Vosk (Offline Speech Recognition) ───────────────────────────────────────
-keep class org.vosk.** { *; }
-keep class com.alphacephei.vosk.** { *; }
-keepclassmembers class org.vosk.** { *; }
-keepclassmembers class com.alphacephei.vosk.** { *; }
-dontwarn org.vosk.**
-dontwarn com.alphacephei.vosk.**

# ── Google Drive & Sign-In ───────────────────────────────────────────────────
-keep class com.google.api.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keepclassmembers class com.google.api.** { *; }
-dontwarn com.google.api.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.auth.**

# ── Gson / JSON ──────────────────────────────────────────────────────────────
-keepattributes EnclosingMethod
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers enum * { *; }

# ── Lottie ───────────────────────────────────────────────────────────────────
-keep class com.airbnb.lottie.** { *; }
-keepclassmembers class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ── Coil (Image loading) ─────────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ── WorkManager ──────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── AppWidget (Vocalize widget) ──────────────────────────────────────────────
-keep class * extends android.appwidget.AppWidgetProvider
-keepclassmembers class * extends android.appwidget.AppWidgetProvider { *; }

# ── BroadcastReceivers & Services ───────────────────────────────────────────
-keep class com.vocalize.app.util.ReminderBroadcastReceiver { *; }
-keep class com.vocalize.app.util.BootReceiver { *; }
-keep class com.vocalize.app.service.PlaybackService { *; }
-keep class com.vocalize.app.service.VoskService { *; }

# ── App models ───────────────────────────────────────────────────────────────
-keep class com.vocalize.app.data.** { *; }

# ── MediaSession / Media3 ────────────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── Misc ─────────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.slf4j.**
-dontwarn org.apache.**
-dontwarn com.fasterxml.**

# Keep line numbers for crash reports
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
