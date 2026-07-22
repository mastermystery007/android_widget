# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn kotlinx.coroutines.**

# Glance callbacks are instantiated by Android/Glance.
-keep class com.mastermystery.oneminutecoach.widget.** { *; }

# Keep worker constructors for WorkManager.
-keep class com.mastermystery.oneminutecoach.worker.** { *; }
