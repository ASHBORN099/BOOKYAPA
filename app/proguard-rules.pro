# Bookyapa ProGuard/R8 Rules

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin serialization
-keep class kotlinx.serialization.** { *; }

# Ktor engine (ServiceLoader pattern)
-keep class io.ktor.client.HttpClient { *; }
-keep class io.ktor.client.HttpClientFactory { *; }
-keep class * extends io.ktor.client.HttpClientFactory
-keep class io.ktor.client.engine.okhttp.** { *; }
-keep class io.ktor.client.HttpClientEngineFactory { *; }
-keep class * implements io.ktor.client.HttpClientEngineFactory { *; }
-keep class io.ktor.client.plugins.** { *; }
-keep class io.ktor.serialization.** { *; }
-keep class io.ktor.http.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }

# OkHttp
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# DataStore Preferences (protobuf internals)
-keep class androidx.datastore.** { *; }
-keep class * extends androidx.datastore.core.Serializer { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.TypeConverter class *
-keep class * extends androidx.room.migration.Migration
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ViewWithFragmentContextWrapper { *; }
-keep class **_HiltModules* { *; }
-keep class **_HiltComponents* { *; }
-keep class **_*Factory { *; }
-keep class **_*MembersInjector { *; }

# Enum safety (protects BookStatus.valueOf(), ThemeMode.valueOf())
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# App data classes
-keep class com.bookyapa.app.data.model.** { *; }
-keep class com.bookyapa.app.data.local.** { *; }

# JSoup
-keeppackagenames org.jsoup.nodes
-dontwarn org.jsoup.**
