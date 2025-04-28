package ir.example.transportationreport.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ir.example.transportationreport.R
import ir.example.transportationreport.data.AppDatabase
import ir.example.transportationreport.data.TransportRepository
import ir.example.transportationreport.databinding.ActivityReportCollectionBinding
import ir.example.transportationreport.ui.viewmodel.TransportViewModel
import ir.example.transportationreport.ui.viewmodel.TransportViewModelFactory
import java.text.NumberFormat
import java.util.Locale

class ReportCollectionActivity : AppCompatActivity() {

    private lateinit var currentItem: List<ReportItem>
    private lateinit var binding: ActivityReportCollectionBinding
    private val viewModel: TransportViewModel by viewModels {
        TransportViewModelFactory(
            TransportRepository.getInstance(
                AppDatabase.getInstance(application).serviceDao(),
                AppDatabase.getInstance(application).driverDao(),
                AppDatabase.getInstance(application).passengerDao()
            )
        )
    }

    sealed class ReportItem {
        data class OverallHeader(val totalServices: Int, val grandTotal: Int) : ReportItem()
        data class DailyHeader(val date: String, val dailyTotal: Int) : ReportItem()
        data class ServiceRow(
            val isHeader: Boolean,
            val col1: String,
            val col2: String,
            val col3: String,
            val col4: String,
            val col5: String,
            val total: String
        ) : ReportItem()
        object Separator : ReportItem()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportCollectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()
        loadData()

        binding.btnExportPdf.setOnClickListener {
            exportPdfWrapper()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ReportCollectionActivity)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = ReportAdapter()
        }
    }

    private fun loadData() {
        viewModel.getAllServicesWithPassengers().observe(this) { services ->
            val items = mutableListOf<ReportItem>().apply {
                add(
                    ReportItem.OverallHeader(
                        totalServices = services.size,
                        grandTotal = services.sumOf { it.service.totalFare }
                    )
                )

                services.groupBy { it.service.date }.forEach { (date, dailyServices) ->
                    add(
                        ReportItem.DailyHeader(
                            date = date,
                            dailyTotal = dailyServices.sumOf { it.service.totalFare }
                        )
                    )

                    dailyServices.forEach { service ->
                        val destinations = (4 downTo 0).map { i ->
                            service.passengers.getOrNull(i)?.destination ?: "-"
                        }
                        add(
                            ReportItem.ServiceRow(
                                isHeader = false,
                                col1 = destinations[0],
                                col2 = destinations[1],
                                col3 = destinations[2],
                                col4 = destinations[3],
                                col5 = destinations[4],
                                total = "کرایه"
                            )
                        )

                        val passengers = (4 downTo 0).map { i ->
                            service.passengers.getOrNull(i)?.name ?: "-"
                        }
                        add(
                            ReportItem.ServiceRow(
                                isHeader = false,
                                col1 = passengers[0],
                                col2 = passengers[1],
                                col3 = passengers[2],
                                col4 = passengers[3],
                                col5 = passengers[4],
                                total = formatCurrency(service.service.totalFare)
                            )
                        )

                        add(ReportItem.Separator)
                    }
                }
            }
            (binding.recyclerView.adapter as ReportAdapter).submitList(items)
            currentItem = items
        }
    }

    private fun exportPdfWrapper() {
        if (checkPermissions()) {
            exportPdf()
        }
    }

    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> true
                else -> {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        101
                    )
                    false
                }
            }
        } else {
            true
        }
    }

    private fun exportPdf() {
        try {
            // PdfExporter(this).exportToPdf(currentItem)
            SimplePdfExporter(this).createTestPdf() // تست نسخه ساده
            Toast.makeText(this, "PDF ایجاد شد", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "خطا در ایجاد PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            101 -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    exportPdf()
                }
            }
        }
    }

    private fun formatCurrency(amount: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(amount) + " تومان"
    }

    private inner class ReportAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var items = listOf<ReportItem>()

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is ReportItem.OverallHeader -> R.layout.item_report_header
                is ReportItem.DailyHeader -> R.layout.item_report_header
                is ReportItem.ServiceRow -> R.layout.item_service_row
                ReportItem.Separator -> R.layout.item_report_separator
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                R.layout.item_report_header -> HeaderViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_report_header, parent, false)
                )
                R.layout.item_service_row -> ServiceRowViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_service_row, parent, false)
                )
                else -> SeparatorViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_report_separator, parent, false)
                )
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is ReportItem.OverallHeader -> (holder as HeaderViewHolder).bindOverall(item)
                is ReportItem.DailyHeader -> (holder as HeaderViewHolder).bindDaily(item)
                is ReportItem.ServiceRow -> (holder as ServiceRowViewHolder).bind(item)
                else -> {}
            }
        }

        override fun getItemCount() = items.size

        fun submitList(newItems: List<ReportItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvHeader: TextView = view.findViewById(R.id.tv_date)

            fun bindOverall(item: ReportItem.OverallHeader) {
                tvHeader.text = "تعداد کل سرویس: ${item.totalServices} | جمع کل: ${formatCurrency(item.grandTotal)}"
                tvHeader.setBackgroundColor(
                    ContextCompat.getColor(tvHeader.context, R.color.primary_color)
                )
            }

            fun bindDaily(item: ReportItem.DailyHeader) {
                tvHeader.text = "${item.date} | جمع روزانه: ${formatCurrency(item.dailyTotal)}"
                tvHeader.setBackgroundColor(
                    ContextCompat.getColor(tvHeader.context, R.color.primary_light)
                )
            }
        }

        inner class ServiceRowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvCol1: TextView = view.findViewById(R.id.tv_col1)
            private val tvCol2: TextView = view.findViewById(R.id.tv_col2)
            private val tvCol3: TextView = view.findViewById(R.id.tv_col3)
            private val tvCol4: TextView = view.findViewById(R.id.tv_col4)
            private val tvCol5: TextView = view.findViewById(R.id.tv_col5)
            private val tvTotal: TextView = view.findViewById(R.id.tv_total)

            fun bind(item: ReportItem.ServiceRow) {
                tvCol1.text = item.col1
                tvCol2.text = item.col2
                tvCol3.text = item.col3
                tvCol4.text = item.col4
                tvCol5.text = item.col5
                tvTotal.text = item.total

                val bgColor = if (item.isHeader) {
                    ContextCompat.getColor(itemView.context, R.color.table_header)
                } else {
                    ContextCompat.getColor(itemView.context, android.R.color.transparent)
                }
                itemView.setBackgroundColor(bgColor)
            }
        }

        inner class SeparatorViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }
}