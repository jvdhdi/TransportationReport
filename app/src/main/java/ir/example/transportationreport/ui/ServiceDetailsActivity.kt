package ir.example.transportationreport.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.setMargins
import androidx.lifecycle.ViewModelProvider
import ir.example.transportationreport.R
import ir.example.transportationreport.data.AppDatabase
import ir.example.transportationreport.data.ServiceWithPassengers
import ir.example.transportationreport.data.TransportRepository
import ir.example.transportationreport.databinding.ActivityServiceDetailsBinding
import ir.example.transportationreport.ui.viewmodel.ServiceDetailsViewModel
import ir.example.transportationreport.ui.viewmodel.ServiceDetailsViewModelFactory
import java.text.NumberFormat
import java.util.Locale

class ServiceDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServiceDetailsBinding
    private lateinit var viewModel: ServiceDetailsViewModel
    private var totalServices = 0
    private var grandTotal = 0
    private val columnWidths = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServiceDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val date = intent.getStringExtra("SELECTED_DATE") ?: ""
        val database = AppDatabase.getInstance(application)
        val repository = TransportRepository.getInstance(
            database.serviceDao(),
            database.driverDao(),
            database.passengerDao()
        )
        val factory = ServiceDetailsViewModelFactory(repository, date)
        viewModel = ViewModelProvider(this, factory)[ServiceDetailsViewModel::class.java]

        setupUI()
        observeData()
    }

    private fun setupUI() {
        binding.btnFinishServices.setOnClickListener { finish() }
        binding.btnAddNewService.setOnClickListener {
            startActivity(Intent(this, AddServiceActivity::class.java).apply {
                putExtra("FORMATTED_DATE", intent.getStringExtra("SELECTED_DATE"))
            })
        }
    }

    private fun observeData() {
        viewModel.servicesWithPassengers.observe(this) { services ->
            services?.let {
                totalServices = it.size
                grandTotal = it.sumOf { swp -> swp.service.totalFare }
                updateHeader()
                calculateColumnWidths(it)
                generateServiceTables(it)
            }
        }
    }

    private fun updateHeader() {
        binding.tvServiceDate.text = "تاریخ: ${intent.getStringExtra("SELECTED_DATE")}"
        binding.tvTotalServices.text = "تعداد سرویس‌ها: $totalServices"
        binding.tvGrandTotal.text = "جمع کل روزانه: ${formatCurrency(grandTotal)}"
    }

    private fun calculateColumnWidths(services: List<ServiceWithPassengers>) {
        columnWidths.clear()

        // ترتیب جدید ستون‌ها: [هزینه, شماره سرویس, مسافر5, مسافر4, مسافر3, مسافر2, مسافر1]
        repeat(7) { columnIndex ->
            var maxWidth = 0
            services.forEach { swp ->
                val text = when (columnIndex) {
                    0 -> formatCurrency(swp.service.totalFare) // هزینه سرویس
                    1 -> "سرویس ${swp.service.id}" // شماره سرویس
                    2 -> swp.passengers.getOrNull(4)?.name ?: "-" // مسافر5
                    3 -> swp.passengers.getOrNull(3)?.name ?: "-" // مسافر4
                    4 -> swp.passengers.getOrNull(2)?.name ?: "-" // مسافر3
                    5 -> swp.passengers.getOrNull(1)?.name ?: "-" // مسافر2
                    6 -> swp.passengers.getOrNull(0)?.name ?: "-" // مسافر1
                    else -> "-"
                }
                val width = calculateTextWidth(text)
                if (width > maxWidth) maxWidth = width
            }
            columnWidths.add((maxWidth * 1.2).toInt())
        }
    }

    private fun calculateTextWidth(text: String): Int {
        return TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
            this.text = text
            measure(0, 0)
        }.measuredWidth
    }

    private fun generateServiceTables(services: List<ServiceWithPassengers>) {
        binding.tablesContainer.removeAllViews()

        services.forEach { swp ->
            val horizontalScroll = HorizontalScrollView(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                isFillViewport = true
            }

            val table = TableLayout(this).apply {
                layoutParams = TableLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dpToPx(8))
                }
            }

            table.addView(createPassengerRow(swp))
            table.addView(createDestinationRow(swp)) // حذف خط جداکننده

            horizontalScroll.addView(table)
            binding.tablesContainer.addView(horizontalScroll)
        }
    }

    private fun createPassengerRow(swp: ServiceWithPassengers): TableRow {
        return TableRow(this).apply {
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )

            // ترتیب جدید: هزینه، شماره سرویس، مسافر5 تا مسافر1
            addView(createAdaptiveCell( // ستون هزینه
                text = formatCurrency(swp.service.totalFare),
                columnIndex = 0
            ))

            addView(createAdaptiveCell( // ستون شماره سرویس
                text = "سرویس ${swp.service.id}",
                columnIndex = 1
            ))

            // مسافرها از 5 تا 1
            for (i in 4 downTo 0) {
                addView(createAdaptiveCell(
                    text = swp.passengers.getOrNull(i)?.name ?: "-",
                    columnIndex = 2 + (4 - i) // 2,3,4,5,6
                ))
            }
        }
    }

    private fun createDestinationRow(swp: ServiceWithPassengers): TableRow {
        return TableRow(this).apply {
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )

            // ستون ویرایش
            addView(createEditButton(swp.service.id).apply {
                layoutParams = TableRow.LayoutParams(
                    columnWidths[0],
                    dpToPx(48))
            })

            // ستون راننده
            addView(createAdaptiveCell(
                text = "راننده: ${swp.service.driverId}",
                columnIndex = 1
            ))

            // مقصدها از 5 تا 1
            for (i in 4 downTo 0) {
                addView(createAdaptiveCell(
                    text = swp.passengers.getOrNull(i)?.destination ?: "-",
                    columnIndex = 2 + (4 - i) // 2,3,4,5,6
                ))
            }
        }
    }

    private fun createAdaptiveCell(text: String, columnIndex: Int): TextView {
        return TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            setBackgroundResource(R.drawable.cell_border)
            layoutParams = TableRow.LayoutParams(
                columnWidths[columnIndex],
                dpToPx(48)
            ).apply {
                marginEnd = dpToPx(2)
            }
            maxLines = 1

        }
    }

    private fun createEditButton(serviceId: Long): TextView {
        return TextView(this).apply {
            text = "ویرایش"
            setTextColor(Color.WHITE)
            setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent))
            gravity = Gravity.CENTER
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            layoutParams = TableRow.LayoutParams(
                columnWidths[0],
                dpToPx(48)
            ).apply {
                marginStart = dpToPx(4)
            }
            setOnClickListener {
                startActivity(Intent(this@ServiceDetailsActivity, AddServiceActivity::class.java).apply {
                    putExtra("SERVICE_ID", serviceId)
                    putExtra("FORMATTED_DATE", intent.getStringExtra("SELECTED_DATE"))
                })
            }
        }
    }

    private fun formatCurrency(amount: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(amount)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}