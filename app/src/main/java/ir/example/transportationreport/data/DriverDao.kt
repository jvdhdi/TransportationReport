package ir.example.transportationreport.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.example.transportationreport.model.Driver
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(driver: Driver)

    @Query("DELETE FROM drivers")
    suspend fun deleteAll()

    @Query("SELECT * FROM drivers")
    fun getAllDrivers(): Flow<List<Driver>>
}