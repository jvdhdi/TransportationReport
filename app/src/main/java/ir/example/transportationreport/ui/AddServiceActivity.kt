package ir.example.transportationreport.ui

import android.text.TextWatcher
import android.text.Editable
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ir.example.transportationreport.data.AppDatabase
import ir.example.transportationreport.data.TransportRepository
import ir.example.transportationreport.databinding.ActivityAddServiceBinding
import ir.example.transportationreport.model.Passenger
import ir.example.transportationreport.model.Service
import ir.example.transportationreport.ui.viewmodel.TransportViewModel
import ir.example.transportationreport.ui.viewmodel.TransportViewModelFactory
import ir.example.transportationreport.utils.Resource
import kotlinx.coroutines.launch
import android.content.Intent
import java.text.NumberFormat
import java.util.Locale

class AddServiceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddServiceBinding
    private val viewModel: TransportViewModel by viewModels {
        TransportViewModelFactory(
            TransportRepository.getInstance(
                AppDatabase.getInstance(application).serviceDao(),
                AppDatabase.getInstance(application).driverDao(),
                AppDatabase.getInstance(application).passengerDao()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dayName = intent.getStringExtra("DAY_NAME") ?: ""
        val formattedDate = intent.getStringExtra("FORMATTED_DATE") ?: ""
        binding.tvSelectedDate.text = "$dayName $formattedDate"

        setupViews()
        setupObservers()
        setupFareListeners()
    }

    private fun setupViews() {
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnSubmit.setOnClickListener { saveService() }
    }

    private fun setupObservers() {
        viewModel.allDrivers.observe(this) { drivers ->
            binding.actvDriver.setAdapter(
                ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,
                    drivers.map { it.name })
            )
        }
    }

    private fun saveService() {
        // بررسی انتخاب راننده
        val driverName = binding.actvDriver.text.toString()
        if (driverName.isBlank()) {
            Toast.makeText(this, "لطفاً نام راننده را انتخاب کنید", Toast.LENGTH_SHORT).show()
            return
        }

        // بررسی وجود راننده در لیست
        val driver = viewModel.allDrivers.value?.find { it.name == driverName }
        if (driver == null) {
            Toast.makeText(this, "راننده انتخاب شده معتبر نیست", Toast.LENGTH_SHORT).show()
            return
        }

        val passengers = listOfNotNull(
            createPassenger(1),
            createPassenger(2),
            createPassenger(3),
            createPassenger(4),
            createPassenger(5)
        ).takeIf { it.isNotEmpty() } ?: run {
            Toast.makeText(this, "حداقل یک مسافر وارد کنید", Toast.LENGTH_SHORT).show()
            return
        }

        val service = Service(
            id = intent.getLongExtra("SERVICE_ID", 0L),
            driverId = driver.id,
            date = intent.getStringExtra("FORMATTED_DATE") ?: "",
            totalFare = passengers.sumOf { it.fare }
        )

        lifecycleScope.launch {
            if (intent.hasExtra("SERVICE_ID")) {
                viewModel.updateServiceWithPassengers(service, passengers).observe(this@AddServiceActivity) {
                    when (it) {
                        is Resource.Success -> {
                            startActivity(Intent(this@AddServiceActivity, ServiceDetailsActivity::class.java).apply {
                                putExtra("SERVICE_ID", service.id)
                                putExtra("SELECTED_DATE", intent.getStringExtra("FORMATTED_DATE"))
                            })
                            finish()
                        }
                        is Resource.Error -> showError(it.message)
                        else -> {}
                    }
                }
            } else {
                viewModel.addServiceWithPassengers(service, passengers).observe(this@AddServiceActivity) {
                    when (it) {
                        is Resource.Success -> {
                            startActivity(Intent(this@AddServiceActivity, ServiceDetailsActivity::class.java).apply {
                                putExtra("SERVICE_ID", it.data)
                                putExtra("SELECTED_DATE", intent.getStringExtra("FORMATTED_DATE"))
                            })
                            finish()
                        }
                        is Resource.Error -> showError(it.message)
                        else -> {}
                    }
                }
            }
        }
    }

    // بقیه متدها بدون تغییر باقی می‌مانند
    private fun createPassenger(number: Int): Passenger? {
        val nameField = when (number) {
            1 -> binding.etPassenger1
            2 -> binding.etPassenger2
            3 -> binding.etPassenger3
            4 -> binding.etPassenger4
            5 -> binding.etPassenger5
            else -> return null
        }

        if (nameField.text.isBlank()) return null

        return Passenger(
            name = nameField.text.toString(),
            destination = when (number) {
                1 -> binding.etDestination1.text.toString()
                2 -> binding.etDestination2.text.toString()
                3 -> binding.etDestination3.text.toString()
                4 -> binding.etDestination4.text.toString()
                5 -> binding.etDestination5.text.toString()
                else -> ""
            },
            fare = when (number) {
                1 -> binding.etFare1.text.toString().toIntOrNull() ?: 0
                2 -> binding.etFare2.text.toString().toIntOrNull() ?: 0
                3 -> binding.etFare3.text.toString().toIntOrNull() ?: 0
                4 -> binding.etFare4.text.toString().toIntOrNull() ?: 0
                5 -> binding.etFare5.text.toString().toIntOrNull() ?: 0
                else -> 0
            },
            serviceId = intent.getLongExtra("SERVICE_ID", 0L)
        )
    }

    private fun setupFareListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateTotal()
            }
        }

        listOf(
            binding.etFare1,
            binding.etFare2,
            binding.etFare3,
            binding.etFare4,
            binding.etFare5
        ).forEach { it.addTextChangedListener(textWatcher) }
    }

    private fun calculateTotal() {
        val fares = listOf(
            binding.etFare1.text.toString().toIntOrNull() ?: 0,
            binding.etFare2.text.toString().toIntOrNull() ?: 0,
            binding.etFare3.text.toString().toIntOrNull() ?: 0,
            binding.etFare4.text.toString().toIntOrNull() ?: 0,
            binding.etFare5.text.toString().toIntOrNull() ?: 0
        )

        val total = fares.sum()
        binding.tvTotal.text = "جمع کل: ${NumberFormat.getNumberInstance(Locale.US).format(total)} تومان"
    }

    private fun showError(message: String?) {
        Toast.makeText(this, message ?: "خطا در عملیات", Toast.LENGTH_SHORT).show()
    }
}