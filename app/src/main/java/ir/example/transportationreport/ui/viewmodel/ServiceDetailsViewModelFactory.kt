package ir.example.transportationreport.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ir.example.transportationreport.data.TransportRepository

class ServiceDetailsViewModelFactory(
    private val repository: TransportRepository,
    private val date: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ServiceDetailsViewModel::class.java)) {
            return ServiceDetailsViewModel(repository, date) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}