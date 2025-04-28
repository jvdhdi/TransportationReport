package ir.example.transportationreport.ui.viewmodel

import androidx.lifecycle.*
import ir.example.transportationreport.data.TransportRepository
import ir.example.transportationreport.model.Driver
import ir.example.transportationreport.model.DriverSummary
import ir.example.transportationreport.model.Passenger
import ir.example.transportationreport.model.Service
import ir.example.transportationreport.data.ServiceDao.MonthlySummary
import ir.example.transportationreport.data.ServiceDao.DailyPerformance
import ir.example.transportationreport.utils.Resource
import ir.example.transportationreport.data.ServiceWithPassengers
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

class TransportViewModel(private val repository: TransportRepository) : ViewModel() {

    // منطقه مشاهده داده‌ها

    val allDrivers: LiveData<List<Driver>> = repository.getAllDrivers().asLiveData()

    fun getMonthlySummary(): LiveData<MonthlySummary> = repository.getMonthlySummary()

    fun getDailyPerformance(): LiveData<List<DailyPerformance>> = repository.getDailyPerformance()

    fun getDriverPerformanceReport(): LiveData<List<DriverSummary>> = repository.getDriverSummary()

    fun getAllServicesWithPassengers(): LiveData<List<ServiceWithPassengers>> =
        repository.getAllServicesWithPassengers()

    // منطقه عملیات سرویس

    fun addServiceWithPassengers(service: Service, passengers: List<Passenger>) =
        repository.addServiceWithPassengers(service, passengers)

    fun loadServiceForEditing(serviceId: Long) =
        repository.getServiceWithPassengers(serviceId).map { Resource.Success(it) }

    fun updateServiceWithPassengers(service: Service, passengers: List<Passenger>) =
        repository.updateServiceWithPassengers(service, passengers)

    // منطقه عملیات راننده

    suspend fun deleteAllDrivers() = repository.deleteAllDrivers()

    suspend fun insertDriver(driver: Driver) = repository.insertDriver(driver)

    // منطقه کارخانه ViewModel

    class TransportViewModelFactory(
        private val repository: TransportRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TransportViewModel::class.java)) {
                return TransportViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}