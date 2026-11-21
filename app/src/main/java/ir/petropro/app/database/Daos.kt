package ir.petropro.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {
    @Query("SELECT * FROM programs ORDER BY serverOrder")
    fun observeAll(): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs")
    suspend fun getAll(): List<ProgramEntity>

    @Query("DELETE FROM programs WHERE id = :id")
    suspend fun delete(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: ProgramEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDays(days: List<DayEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemEntity>)

    @Query("SELECT * FROM days WHERE programId = :programId ORDER BY dayIndex")
    fun observeDays(programId: Int): Flow<List<DayEntity>>

    @Query("SELECT COUNT(*) FROM items WHERE dayId = :dayId")
    suspend fun itemCount(dayId: Int): Int

    @Query("SELECT * FROM days WHERE id = :dayId")
    fun observeDay(dayId: Int): Flow<DayEntity?>

    @Query("SELECT * FROM items WHERE dayId = :dayId ORDER BY orderIndex, id")
    fun observeItems(dayId: Int): Flow<List<ItemEntity>>

    @Query("SELECT d.id AS dayId, COUNT(i.id) AS itemCount FROM days d LEFT JOIN items i ON i.dayId = d.id WHERE d.programId = :programId GROUP BY d.id")
    fun observeDayCounts(programId: Int): Flow<List<DayItemCount>>
}

data class DayItemCount(val dayId: Int, val itemCount: Int)

@Dao
interface SetLogDao {
    @Query("SELECT * FROM set_logs WHERE date = :date")
    fun observeForDate(date: String): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM set_logs WHERE date = :date")
    suspend fun getForDate(date: String): List<SetLogEntity>

    @Query("SELECT * FROM set_logs WHERE itemId = :itemId AND setNumber = :setNumber AND date = :date")
    suspend fun get(itemId: Int, setNumber: Int, date: String): SetLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: SetLogEntity)

    @Query("DELETE FROM set_logs WHERE itemId = :itemId AND setNumber = :setNumber AND date = :date")
    suspend fun delete(itemId: Int, setNumber: Int, date: String)

    @Query("UPDATE set_logs SET dirty = 0 WHERE itemId = :itemId AND setNumber = :setNumber AND date = :date")
    suspend fun markClean(itemId: Int, setNumber: Int, date: String)

}

@Dao
interface CheckinDao {
    @Query("SELECT * FROM checkins WHERE date = :date")
    fun observe(date: String): Flow<CheckinEntity?>

    @Query("SELECT * FROM checkins WHERE date = :date")
    suspend fun get(date: String): CheckinEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(checkin: CheckinEntity)

    @Query("UPDATE checkins SET dirty = 0 WHERE date = :date")
    suspend fun markClean(date: String)
}

@Dao
interface OutboxDao {
    @Insert
    suspend fun insert(op: OutboxEntity): Long

    @Update
    suspend fun update(op: OutboxEntity)

    @Query("SELECT * FROM outbox WHERE state = 'PENDING' ORDER BY seq LIMIT 1")
    suspend fun firstPending(): OutboxEntity?

    @Query("DELETE FROM outbox WHERE seq = :seq")
    suspend fun delete(seq: Long)

    @Query("SELECT COUNT(*) FROM outbox WHERE state IN ('PENDING','BLOCKED')")
    fun observeUnsentCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM outbox WHERE state IN ('PENDING','BLOCKED')")
    suspend fun unsentCount(): Int

    @Query("SELECT COUNT(*) FROM outbox WHERE state = 'PENDING' AND refKey = :refKey AND seq > :seq")
    suspend fun laterPendingCount(refKey: String, seq: Long): Int

    @Query("SELECT COUNT(*) FROM outbox WHERE state = 'PENDING' AND refKey = :refKey")
    suspend fun pendingCountFor(refKey: String): Int

    @Query("SELECT * FROM outbox WHERE state IN ('CONFLICT','REJECTED','BLOCKED') ORDER BY seq")
    fun observeIssues(): Flow<List<OutboxEntity>>

    @Query("UPDATE outbox SET state = 'PENDING', nextAttemptAt = 0 WHERE state = 'BLOCKED'")
    suspend fun retryBlocked()

    @Query("DELETE FROM outbox WHERE state IN ('CONFLICT','REJECTED')")
    suspend fun dismissIssues()

    @Query("SELECT COUNT(*) FROM outbox WHERE state = 'PENDING'")
    suspend fun pendingCount(): Int
}

@Dao
interface MediaDao {
    @Query("SELECT * FROM media WHERE url = :url")
    suspend fun get(url: String): MediaEntity?

    @Query("SELECT * FROM media")
    suspend fun all(): List<MediaEntity>

    @Query("SELECT * FROM media WHERE state IN ('NEEDED','FAILED','PAUSED_LOW_STORAGE') AND attempts < :maxAttempts ORDER BY url")
    suspend fun downloadable(maxAttempts: Int): List<MediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(media: MediaEntity)

    @Query("DELETE FROM media WHERE url = :url")
    suspend fun delete(url: String)

    @Query("DELETE FROM media")
    suspend fun deleteAll()

    @Query("SELECT url, localPath FROM media WHERE state = 'DONE'")
    fun observeDone(): Flow<List<DoneMedia>>

    @Query("SELECT COUNT(*) FROM media WHERE state = 'DONE'")
    fun observeDoneCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM media WHERE state = 'DONE'")
    fun observeDoneBytes(): Flow<Long>
}
