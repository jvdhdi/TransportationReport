package ir.example.transportationreport.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ir.example.transportationreport.data.TransportRepository

class TransportViewModelFactory(
    private val repository: TransportRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TransportViewModel(repository) as T
    }
}