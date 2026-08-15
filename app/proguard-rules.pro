# R8 rules for the release build.
#
# The default `proguard-android-optimize.txt` covers the platform. Everything
# here exists because something in this app is reached by reflection or by a
# generated lookup, and R8 cannot see either.
#
# The rule for this file: no blanket keeps. A `-keep class com.kamsiob.**`
# would make every problem below go away and would also make shrinking
# pointless, which matters for an app people install over mobile data.

# ---------- kotlinx.serialization ----------
#
# Serializers are generated as companion objects and found by name at runtime,
# so the lookup is invisible to R8. Without these the Subsonic responses, the
# Surroundings manifest and the export file all fail to parse in release and
# work perfectly in debug, which is the worst shape a bug can have.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class **$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn kotlinx.serialization.**

# The tolerant serializers are referenced from annotations rather than from
# code, so nothing calls their constructors in a way R8 can trace.
-keep class com.kamsiob.meedwell.core.subsonic.Tolerant* { *; }

# ---------- Room ----------
#
# Room's generated implementations are looked up by name from the abstract
# class, and its entities are read field by field by generated code.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ---------- Media3 ----------
#
# The session and the notification are built by the platform from a component
# name in the manifest, so nothing in this app references the service class
# directly.
-keep class com.kamsiob.meedwell.playback.PlaybackService { *; }
-dontwarn androidx.media3.**

# ---------- OkHttp and Okio ----------
#
# Both reference optional platform pieces that are absent on Android. These are
# warnings about code that is never reached rather than about anything missing.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn okio.**

# ---------- Keeping the crash reports readable ----------
#
# Line numbers survive so a stack trace from a release build can be read against
# the source. The file name is replaced because it adds nothing once the line
# numbers are there.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
