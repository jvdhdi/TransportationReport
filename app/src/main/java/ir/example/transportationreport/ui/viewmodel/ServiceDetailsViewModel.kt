package ir.example.transportationreport.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import ir.example.transportationreport.data.ServiceWithPassengers
import ir.example.transportationreport.data.TransportRepository

class ServiceDetailsViewModel(
    private val repository: TransportRepository,
    private val date: String
) : ViewModel() {

    val servicesWithPassengers: LiveData<List<ServiceWithPassengers>> =
        repository.getServicesWithPassengersByDate(date)
}