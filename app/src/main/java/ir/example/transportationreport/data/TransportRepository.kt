package ir.example.transportationreport.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import ir.example.transportationreport.data.ServiceDao.DailyPerformance
import ir.example.transportationreport.data.ServiceDao.MonthlySummary
import ir.example.transportationreport.model.DriverSummary
import ir.example.transportationreport.model.Driver
import ir.example.transportationreport.model.Passenger
import ir.example.transportationreport.model.Service
import ir.example.transportationreport.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class TransportRepository private constructor(
    private val serviceDao: ServiceDao,
    private val driverDao: DriverDao,
    private val passengerDao: PassengerDao
) {

    fun getAllDrivers(): Flow<List<Driver>> = driverDao.getAllDrivers()

    fun getServicesWithPassengersByDate(date: String): LiveData<List<ServiceWithPassengers>> {
        return serviceDao.getServicesWithPassengersByDate(date)
    }

    suspend fun getPassengersForService(serviceId: Long) =
        passengerDao.getPassengersByServiceId(serviceId).value ?: emptyList()

    fun addServiceWithPassengers(
        service: Service,
        passengers: List<Passenger>
    ): LiveData<Resource<Long>> = liveData(Dispatchers.IO) {
        emit(Resource.Loading())
        try {
            val serviceId = serviceDao.insert(service)
            passengerDao.insertAll(passengers.map { it.copy(serviceId = serviceId) })
            emit(Resource.Success(serviceId))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "خطا در ثبت سرویس"))
        }
    }

    fun getServiceWithPassengers(serviceId: Long): LiveData<ServiceWithPassengers> {
        return serviceDao.getServiceWithPassengers(serviceId)
    }

    fun updateServiceWithPassengers(
        service: Service,
        passengers: List<Passenger>
    ): LiveData<Resource<Unit>> = liveData(Dispatchers.IO) {
        emit(Resource.Loading())
        try {
            serviceDao.update(service)
            passengerDao.deleteByServiceId(service.id)
            passengerDao.insertAll(passengers)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "خطا در بروزرسانی سرویس"))
        }
    }

    suspend fun deleteAllDrivers() = driverDao.deleteAll()

    suspend fun insertDriver(driver: Driver) = driverDao.insert(driver)

    companion object {
        @Volatile private var instance: TransportRepository? = null

        fun getInstance(
            serviceDao: ServiceDao,
            driverDao: DriverDao,
            passengerDao: PassengerDao
        ): TransportRepository {
            return instance ?: synchronized(this) {
                instance ?: TransportRepository(serviceDao, driverDao, passengerDao).also {
                    instance = it
                }
            }
        }
    }

    fun getAllServicesWithPassengers(): LiveData<List<ServiceWithPassengers>> {
        return serviceDao.getAllServicesWithPassengers()
    }

    fun getDriverSummary(): LiveData<List<DriverSummary>> {
        return serviceDao.getDriverSummary()
    }

    fun getMonthlySummary(): LiveData<MonthlySummary> = serviceDao.getMonthlySummary()

    fun getDailyPerformance(): LiveData<List<DailyPerformance>> = serviceDao.getDailyPerformance()
}