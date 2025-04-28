package ir.example.transportationreport.ui

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ir.example.transportationreport.data.AppDatabase
import ir.example.transportationreport.data.TransportRepository
import ir.example.transportationreport.databinding.ActivitySettingsBinding
import ir.example.transportationreport.model.Driver
import ir.example.transportationreport.ui.viewmodel.TransportViewModel
import ir.example.transportationreport.ui.viewmodel.TransportViewModelFactory
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: TransportViewModel by viewModels {
        val database = AppDatabase.getInstance(application)
        TransportViewModelFactory(
            TransportRepository.getInstance(
                database.serviceDao(),
                database.driverDao(),
                database.passengerDao()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEditTextFocus()
        loadSavedDrivers()

        binding.btnSave.setOnClickListener {
            if (validateInputs()) {
                saveDrivers()
                Toast.makeText(this, "تنظیمات ذخیره شد", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupEditTextFocus() {
        binding.etDriver1.apply {
            imeOptions = EditorInfo.IME_ACTION_NEXT
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    binding.etDriver2.requestFocus()
                    true
                } else false
            }
        }

        binding.etDriver2.apply {
            imeOptions = EditorInfo.IME_ACTION_NEXT
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    binding.etDriver3.requestFocus()
                    true
                } else false
            }
        }

        binding.etDriver3.apply {
            imeOptions = EditorInfo.IME_ACTION_NEXT
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    binding.etDriver4.requestFocus()
                    true
                } else false
            }
        }

        binding.etDriver4.apply {
            imeOptions = EditorInfo.IME_ACTION_NEXT
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    binding.etDriver5.requestFocus()
                    true
                } else false
            }
        }

        binding.etDriver5.apply {
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard()
                    true
                } else false
            }
        }
    }

    private fun validateInputs(): Boolean {
        return when {
            binding.etDriver1.text.isNullOrEmpty() -> {
                showError("نام راننده ۱ الزامی است!")
                false
            }
            binding.etDriver2.text.isNullOrEmpty() -> {
                showError("نام راننده ۲ الزامی است!")
                false
            }
            binding.etDriver3.text.isNullOrEmpty() -> {
                showError("نام راننده ۳ الزامی است!")
                false
            }
            else -> true
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.apply {
            hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
    }

    private fun saveDrivers() {
        getSharedPreferences("DriverSettings", Context.MODE_PRIVATE).edit().apply {
            putString("DRIVER_1", binding.etDriver1.text.toString())
            putString("DRIVER_2", binding.etDriver2.text.toString())
            putString("DRIVER_3", binding.etDriver3.text.toString())
            putString("DRIVER_4", binding.etDriver4.text.toString())
            putString("DRIVER_5", binding.etDriver5.text.toString())
            apply()
        }

        lifecycleScope.launch {
            viewModel.deleteAllDrivers()
            listOf(
                binding.etDriver1.text.toString(),
                binding.etDriver2.text.toString(),
                binding.etDriver3.text.toString(),
                binding.etDriver4.text.toString(),
                binding.etDriver5.text.toString()
            ).filter { it.isNotBlank() }.forEach { name ->
                viewModel.insertDriver(Driver(name = name))
            }
        }
    }

    private fun loadSavedDrivers() {
        val prefs = getSharedPreferences("DriverSettings", Context.MODE_PRIVATE)
        binding.etDriver1.setText(prefs.getString("DRIVER_1", ""))
        binding.etDriver2.setText(prefs.getString("DRIVER_2", ""))
        binding.etDriver3.setText(prefs.getString("DRIVER_3", ""))
        binding.etDriver4.setText(prefs.getString("DRIVER_4", ""))
        binding.etDriver5.setText(prefs.getString("DRIVER_5", ""))
    }
}