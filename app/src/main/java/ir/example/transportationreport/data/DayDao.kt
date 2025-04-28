package ir.example.transportationreport.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ir.example.transportationreport.model.Day

@Dao
interface DayDao {
    @Insert
    suspend fun insert(day: Day)

    @Query("SELECT * FROM days")
    fun getAllDays(): List<Day>
}