package ir.example.transportationreport.data

import androidx.lifecycle.LiveData
import androidx.room.*
import ir.example.transportationreport.model.Passenger

@Dao
interface PassengerDao {
    @Query("SELECT * FROM passengers WHERE serviceId = :serviceId")
    fun getPassengersByServiceId(serviceId: Long): LiveData<List<Passenger>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(passengers: List<Passenger>)

    @Query("DELETE FROM passengers WHERE serviceId = :serviceId")
    suspend fun deleteByServiceId(serviceId: Long)
}