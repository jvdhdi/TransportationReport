package ir.example.transportationreport.data

import androidx.lifecycle.LiveData
import androidx.room.*
import ir.example.transportationreport.model.DriverSummary
import ir.example.transportationreport.model.Service
import ir.example.transportationreport.data.ServiceWithPassengers

@Dao
interface ServiceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(service: Service): Long

    @Update
    suspend fun update(service: Service)

    @Query("DELETE FROM services WHERE id = :serviceId")
    suspend fun deleteById(serviceId: Long)

    @Query("SELECT * FROM services WHERE date = :date")
    fun getServicesByDate(date: String): LiveData<List<Service>>

    @Transaction
    @Query("SELECT * FROM services WHERE id = :serviceId")
    fun getServiceWithPassengers(serviceId: Long): LiveData<ServiceWithPassengers>

    @Query("SELECT * FROM services WHERE id = :serviceId")
    fun getServiceById(serviceId: Long): LiveData<Service>

    @Transaction
    @Query("SELECT * FROM services WHERE date = :date")
    fun getServicesWithPassengersByDate(date: String): LiveData<List<ServiceWithPassengers>>

    @Transaction
    @Query("SELECT * FROM services ORDER BY date DESC")
    fun getAllServicesWithPassengers(): LiveData<List<ServiceWithPassengers>>

    @Query("""
        SELECT
            services.driverId,
            drivers.name as driverName, 
            COUNT(*) as totalServices,
            SUM(totalFare) as totalFares
        FROM services
        INNER JOIN drivers ON services.driverId = drivers.id  
        GROUP BY services.driverId
    """)
    fun getDriverSummary(): LiveData<List<DriverSummary>>

    @Query("""
        SELECT
            COUNT(*) as totalServices,
            SUM(totalFare) as totalFares
        FROM services
    """)
    fun getMonthlySummary(): LiveData<MonthlySummary>

    @Query("""
        SELECT
            date,
            driverId,
            COUNT(*) as services,
            SUM(totalFare) as fares
        FROM services
        GROUP BY date, driverId
        ORDER BY date DESC
    """)
    fun getDailyPerformance(): LiveData<List<DailyPerformance>>

    data class MonthlySummary(
        val totalServices: Int,
        val totalFares: Int
    )

    data class DailyPerformance(
        val date: String,
        val driverId: Long,
        val services: Int,
        val fares: Int
    )
}