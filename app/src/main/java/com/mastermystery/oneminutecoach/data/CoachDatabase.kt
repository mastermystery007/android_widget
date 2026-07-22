package com.mastermystery.oneminutecoach.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

enum class GoalCategory { WORK, STUDY, HEALTH, CREATIVE, HOME, PERSONAL }
enum class EnergyLevel { LOW, MEDIUM, HIGH }
enum class CoachContext { ANY, HOME, WORK, COMMUTE, OUTDOORS }
enum class SessionOutcome { COMPLETED, PARTIAL, SKIPPED }

@Entity(
    tableName = "goals",
    indices = [Index(value = ["isActive"])],
)
data class GoalEntity(
    @androidx.room.PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val outcome: String = "",
    val category: GoalCategory,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "actions",
    indices = [Index(value = ["goalId"])],
)
data class ActionEntity(
    @androidx.room.PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: Long,
    val title: String,
    val durationMinutes: Int,
    val energy: EnergyLevel,
    val context: CoachContext,
    val morningFriendly: Boolean = true,
    val eveningFriendly: Boolean = true,
    val completedCount: Int = 0,
    val skippedCount: Int = 0,
    val lastShownAt: Long? = null,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "sessions",
    indices = [Index(value = ["goalId"]), Index(value = ["actionId"]), Index(value = ["endedAt"])],
)
data class SessionEntity(
    @androidx.room.PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: Long,
    val actionId: Long?,
    val actionTitle: String,
    val plannedMinutes: Int,
    val actualSeconds: Int,
    val outcome: SessionOutcome,
    val startedAt: Long,
    val endedAt: Long,
    val note: String = "",
)

@Entity(tableName = "current_suggestion")
data class SuggestionEntity(
    @androidx.room.PrimaryKey
    val id: Int = 1,
    val goalId: Long,
    val goalTitle: String,
    val actionId: Long,
    val actionTitle: String,
    val durationMinutes: Int,
    val reason: String,
    val generatedAt: Long = System.currentTimeMillis(),
)

data class CoachPreferences(
    val availableMinutes: Int = 3,
    val energy: EnergyLevel = EnergyLevel.MEDIUM,
    val context: CoachContext = CoachContext.ANY,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val hapticsEnabled: Boolean = true,
    val dynamicColorEnabled: Boolean = true,
    val showReasonInWidget: Boolean = true,
)

data class WidgetSnapshot(
    val suggestion: SuggestionEntity? = null,
    val currentStreak: Int = 0,
    val completedToday: Int = 0,
    val showReason: Boolean = true,
)

class EnumConverters {
    @TypeConverter fun fromGoalCategory(value: GoalCategory): String = value.name
    @TypeConverter fun toGoalCategory(value: String): GoalCategory = GoalCategory.valueOf(value)
    @TypeConverter fun fromEnergy(value: EnergyLevel): String = value.name
    @TypeConverter fun toEnergy(value: String): EnergyLevel = EnergyLevel.valueOf(value)
    @TypeConverter fun fromContext(value: CoachContext): String = value.name
    @TypeConverter fun toContext(value: String): CoachContext = CoachContext.valueOf(value)
    @TypeConverter fun fromOutcome(value: SessionOutcome): String = value.name
    @TypeConverter fun toOutcome(value: String): SessionOutcome = SessionOutcome.valueOf(value)
}

@Dao
interface CoachDao {
    @Query("SELECT * FROM goals ORDER BY isActive DESC, createdAt DESC")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE isActive = 1 LIMIT 1")
    fun observeActiveGoal(): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveGoal(): GoalEntity?

    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    suspend fun getGoals(): List<GoalEntity>

    @Insert
    suspend fun insertGoal(goal: GoalEntity): Long

    @Query("UPDATE goals SET isActive = 0")
    suspend fun deactivateAllGoals()

    @Query("UPDATE goals SET isActive = CASE WHEN id = :goalId THEN 1 ELSE 0 END")
    suspend fun activateGoal(goalId: Long)

    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteGoal(goalId: Long)

    @Query("DELETE FROM actions WHERE goalId = :goalId")
    suspend fun deleteActionsForGoal(goalId: Long)

    @Query("DELETE FROM sessions WHERE goalId = :goalId")
    suspend fun deleteSessionsForGoal(goalId: Long)

    @Query("SELECT * FROM actions WHERE goalId = :goalId ORDER BY createdAt ASC")
    fun observeActions(goalId: Long): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions WHERE goalId = :goalId ORDER BY createdAt ASC")
    suspend fun getActionsForGoal(goalId: Long): List<ActionEntity>

    @Insert
    suspend fun insertAction(action: ActionEntity): Long

    @Insert
    suspend fun insertActions(actions: List<ActionEntity>)

    @Query("UPDATE actions SET isEnabled = :enabled WHERE id = :actionId")
    suspend fun setActionEnabled(actionId: Long, enabled: Boolean)

    @Query("DELETE FROM actions WHERE id = :actionId")
    suspend fun deleteAction(actionId: Long)

    @Query(
        """
        UPDATE actions
        SET completedCount = completedCount + 1,
            lastShownAt = :now
        WHERE id = :actionId
        """,
    )
    suspend fun markActionCompleted(actionId: Long, now: Long)

    @Query(
        """
        UPDATE actions
        SET skippedCount = skippedCount + 1,
            lastShownAt = :now
        WHERE id = :actionId
        """,
    )
    suspend fun markActionSkipped(actionId: Long, now: Long)

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Query("SELECT * FROM sessions ORDER BY endedAt DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int = 100): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY endedAt DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int = 100): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE endedAt >= :fromEpochMillis ORDER BY endedAt DESC")
    suspend fun getSessionsSince(fromEpochMillis: Long): List<SessionEntity>

    @Query("SELECT * FROM current_suggestion WHERE id = 1")
    fun observeSuggestion(): Flow<SuggestionEntity?>

    @Query("SELECT * FROM current_suggestion WHERE id = 1")
    suspend fun getSuggestion(): SuggestionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSuggestion(suggestion: SuggestionEntity)

    @Query("DELETE FROM current_suggestion")
    suspend fun clearSuggestion()
}

@Database(
    entities = [
        GoalEntity::class,
        ActionEntity::class,
        SessionEntity::class,
        SuggestionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(EnumConverters::class)
abstract class CoachDatabase : RoomDatabase() {
    abstract fun coachDao(): CoachDao

    companion object {
        @Volatile private var instance: CoachDatabase? = null

        fun get(context: Context): CoachDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CoachDatabase::class.java,
                    "coach.db",
                ).build().also { instance = it }
            }
        }
    }
}
