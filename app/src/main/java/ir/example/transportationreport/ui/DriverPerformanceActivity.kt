package ir.example.transportationreport.ui

import android.Manifest
import android.content.pm.PackageManager
import com.itextpdf.kernel.colors.DeviceRgb
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.properties.Property
import com.itextpdf.layout.properties.BaseDirection
import ir.example.transportationreport.R
import ir.example.transportationreport.data.AppDatabase
import ir.example.transportationreport.data.ServiceDao.DailyPerformance
import ir.example.transportationreport.data.TransportRepository
import ir.example.transportationreport.databinding.ActivityDriverPerformanceBinding
import ir.example.transportationreport.model.DriverSummary
import ir.example.transportationreport.ui.viewmodel.TransportViewModel
import ir.example.transportationreport.ui.viewmodel.TransportViewModelFactory
import java.io.File
import java.io.FileOutputStream
import java.lang.RuntimeException
import java.text.NumberFormat
import java.util.Locale
class DriverPerformanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDriverPerformanceBinding
    private val viewModel: TransportViewModel by viewModels {
        TransportViewModelFactory(
            TransportRepository.getInstance(
                AppDatabase.getInstance(application).serviceDao(),
                AppDatabase.getInstance(application).driverDao(),
                AppDatabase.getInstance(application).passengerDao()
            )
        )
    }

    private lateinit var dailyAdapter: DailyPerformanceAdapter
    private val driverMap = mutableMapOf<Long, String>()
    private lateinit var persianFont: PdfFont
    private lateinit var persianFontBold: PdfFont
    private val textColor = ColorConstants.BLACK
    private val headerColor = DeviceRgb(63, 81, 181)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverPerformanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        setupFonts()
        setupObservers()
    }

    private fun initViews() {
        dailyAdapter = DailyPerformanceAdapter()
        binding.rvDailyPerformance.apply {
            layoutManager = LinearLayoutManager(this@DriverPerformanceActivity)
            adapter = dailyAdapter
        }

        binding.btnExportPdf.setOnClickListener {
            checkPermissionsAndExport()
        }
    }

    private fun setupFonts() {
        try {
            val assetManager = resources.assets
            val fontNormalStream = assetManager.open("fonts/Vazir.ttf")
              persianFont = PdfFontFactory.createFont(
                    fontNormalStream.readBytes(),
                    PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED
                ) .apply {}

            val fontBoldStream = assetManager.open("fonts/Vazir_Bold.ttf")
           persianFontBold = PdfFontFactory.createFont(
               fontBoldStream.readBytes(),
               PdfEncodings.IDENTITY_H,
               PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED
           ).apply {}
        } catch (e: Exception) {
            Log.e("PDF_FONT", "Error loading fonts: ${e.stackTraceToString()}")
            Toast.makeText(this, "خطا در ایجاد PDF", Toast.LENGTH_LONG).show()
            throw RuntimeException("خطا در ایجاد PDF", e)
        }
    }

    private fun setupObservers() {
        viewModel.allDrivers.observe(this) { drivers ->
            driverMap.clear()
            drivers.forEach { driver ->
                driverMap[driver.id] = driver.name
            }
            updateDriverHeaders()
        }

        viewModel.getMonthlySummary().observe(this) { summary ->
            binding.tvTotalServices.text = summary.totalServices.toString()
            binding.tvTotalFares.text = formatCurrency(summary.totalFares)
        }

        viewModel.getDriverPerformanceReport().observe(this) { summaries ->
            updateDriverSummaryUI(summaries)
        }

        viewModel.getDailyPerformance().observe(this) { performances ->
            processDailyPerformance(performances)
        }
    }

    private fun updateDriverHeaders() {
        binding.apply {
            tvDriver1Header.text = driverMap[1] ?: "راننده ۱"
            tvDriver2Header.text = driverMap[2] ?: "راننده ۲"
            tvDriver3Header.text = driverMap[3] ?: "راننده ۳"
            tvDriver4Header.text = driverMap[4] ?: "راننده ۴"
            tvDriver5Header.text = driverMap[5] ?: "راننده ۵"
        }
    }

    private fun updateDriverSummaryUI(summaries: List<DriverSummary>) {
        summaries.forEach { summary ->
            when (summary.driverId) {
                1L -> updateDriverUI(binding.tvDriver1Services, binding.tvDriver1Fares, summary)
                2L -> updateDriverUI(binding.tvDriver2Services, binding.tvDriver2Fares, summary)
                3L -> updateDriverUI(binding.tvDriver3Services, binding.tvDriver3Fares, summary)
                4L -> updateDriverUI(binding.tvDriver4Services, binding.tvDriver4Fares, summary)
                5L -> updateDriverUI(binding.tvDriver5Services, binding.tvDriver5Fares, summary)
            }
        }
    }

    private fun updateDriverUI(tvServices: TextView, tvFares: TextView, summary: DriverSummary) {
        tvServices.text = summary.totalServices.toString()
        tvFares.text = formatCurrency(summary.totalFares)
    }

    private fun processDailyPerformance(performances: List<DailyPerformance>) {
        val groupedData = performances.groupBy { it.date }
        val processedData = groupedData.map { entry ->
            DailyPerformanceGroup(
                date = entry.key,
                drivers = entry.value.associateBy { it.driverId }
            )
        }
        dailyAdapter.submitList(processedData.sortedByDescending { it.date })
    }

    private fun checkPermissionsAndExport() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                101
            )
        } else {
            exportToPdf()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            exportToPdf()
        }
    }

    private fun exportToPdf() {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "DriverReport_${System.currentTimeMillis()}.pdf"
            val file = File(downloadsDir, fileName)

            PdfWriter(FileOutputStream(file)).use { writer ->
                PdfDocument(writer).use { pdfDocument ->
                    Document(pdfDocument, PageSize.A4.rotate()).use { document ->
                        document.setMargins(20f, 20f, 20f, 20f)
                        document.setFont(persianFont)
                        document.setProperty(Property.BASE_DIRECTION, BaseDirection.RIGHT_TO_LEFT)
                        addHeader(document)
                        addMonthlySummary(document)
                        addDriverSummary(document)
                        addDailyPerformanceTable(document)


                    }
                }
            }
            Toast.makeText(this, "PDF ذخیره شد: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "خطا در ایجاد PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addHeader(document: Document) {
        val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(20f)

        val logo = Image(ImageDataFactory.create(resources.openRawResource(R.raw.app_logo).readBytes()))
            .setWidth(100f)
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            val title = Paragraph("گزارش عملکرد رانندگان")
            .setFont(persianFontBold)
            .setFontSize(18f)
            .setFontColor(headerColor)
            .setTextAlignment(TextAlignment.RIGHT)

        val subtitle = Paragraph("تاریخ گزارش: ${getCurrentDate()}")
            .setFont(persianFont)
            .setFontSize(12f)
            .setTextAlignment(TextAlignment.RIGHT)

        // اصلاح Border
        headerTable.addCell(Cell()
            .add(title)
            .add(subtitle)
            .setBorder(null)
            .setPaddingRight(10f))

        document.add(headerTable)
        document.add(Paragraph("\n"))
    }

    private fun addMonthlySummary(document: Document) {
        val summary = viewModel.getMonthlySummary().value ?: return

        val table = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(15f)

        table.addHeaderCell(createHeaderCell("جمع کل ماهانه", 2))

        addSummaryRow(table, "تعداد کل سرویس‌ها:", summary.totalServices.toString())
        addSummaryRow(table, "مجموع درآمد کل:", "${formatCurrency(summary.totalFares)} تومان")

        document.add(table)
        document.add(Paragraph("\n"))
    }

    private fun addDriverSummary(document: Document) {
        val summaries = viewModel.getDriverPerformanceReport().value ?: return

        val table = Table(UnitValue.createPercentArray(floatArrayOf(40f, 30f, 30f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(15f)

        table.addHeaderCell(createHeaderCell("آمار عملکرد رانندگان", 3))
        table.addHeaderCell(createHeaderCell("نام راننده"))
        table.addHeaderCell(createHeaderCell("تعداد سرویس"))
        table.addHeaderCell(createHeaderCell("جمع درآمد"))

        summaries.forEach { summary ->
            table.addCell(createCell(driverMap[summary.driverId] ?: "نامشخص"))
            table.addCell(createCell(summary.totalServices.toString()))
            table.addCell(createCell(formatCurrency(summary.totalFares)))
        }

        document.add(table)
        document.add(Paragraph("\n"))
    }

    private fun addDailyPerformanceTable(document: Document) {
        val dailyData = dailyAdapter.getItems()
        if (dailyData.isEmpty()) return

        val numColumns = driverMap.size + 1
        val columnWidths = FloatArray(numColumns) { 100f / numColumns }

        val table = Table(UnitValue.createPercentArray(columnWidths))
            .setWidth(UnitValue.createPercentValue(100f))

        table.addHeaderCell(createHeaderCell("عملکرد روزانه", numColumns))
        table.addHeaderCell(createHeaderCell("تاریخ"))
        driverMap.values.sorted().forEach { name ->
            table.addHeaderCell(createHeaderCell(name))
        }

        dailyData.forEach { group ->
            table.addCell(createCell(group.date))
            driverMap.keys.sorted().forEach { driverId ->
                val data = group.drivers[driverId]
                val cellContent = if (data != null) {
                    "${data.services} سرویس\n${formatCurrency(data.fares)} تومان"
                } else {
                    "۰\n۰ تومان"
                }
                table.addCell(createCell(cellContent))
            }
        }

        document.add(table)
    }

    private fun createHeaderCell(text: String, colSpan: Int = 1): Cell {
        return Cell(1, colSpan)
            .add(Paragraph()
                .add(Text(text)
                   .setFont(persianFontBold)
                   .setFontSize(12f)
                   .setFontColor(ColorConstants.WHITE)
                .setBaseDirection(BaseDirection.RIGHT_TO_LEFT)
            )
            .setBackgroundColor(headerColor)
            .setPadding(8f)
            .setTextAlignment(TextAlignment.RIGHT))
    }

    private fun createCell(text: String): Cell {
        return Cell()
            .add(Paragraph()
                .add(Text(text)
                    .setFont(persianFont)
                    .setFontSize(10f)
                    .setFontColor(textColor)
                    .setBaseDirection(BaseDirection.RIGHT_TO_LEFT)
                )
                .setTextAlignment(TextAlignment.RIGHT)
            .setPadding(6f)
            .setBorderBottom(SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
            )}

    private fun addSummaryRow(table: Table, label: String, value: String) {
        table.addCell(Cell()
            .add(Paragraph(label).setFont(persianFontBold))
            .setPadding(6f)
            .setTextAlignment(TextAlignment.RIGHT))

        table.addCell(Cell()
            .add(Paragraph(value).setFont(persianFont))
            .setPadding(6f)
            .setTextAlignment(TextAlignment.RIGHT)
            .setPadding(6f))
    }

    private fun formatCurrency(amount: Int): String {
        val formatter = NumberFormat.getNumberInstance(Locale("fa", "IR"))
        return formatter.format(amount)
    }

    private fun getCurrentDate(): String {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        return "$year/${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}"
    }

    private inner class DailyPerformanceAdapter :
        RecyclerView.Adapter<DailyPerformanceAdapter.ViewHolder>() {

        private var items = listOf<DailyPerformanceGroup>()

        fun submitList(newItems: List<DailyPerformanceGroup>) {
            items = newItems
            notifyDataSetChanged()
        }

        fun getItems() = items

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_daily_performance, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
            private val tvDriver1: TextView = itemView.findViewById(R.id.tv_driver1)
            private val tvDriver2: TextView = itemView.findViewById(R.id.tv_driver2)
            private val tvDriver3: TextView = itemView.findViewById(R.id.tv_driver3)
            private val tvDriver4: TextView = itemView.findViewById(R.id.tv_driver4)
            private val tvDriver5: TextView = itemView.findViewById(R.id.tv_driver5)

            fun bind(item: DailyPerformanceGroup) {
                tvDate.text = item.date
                updateDriverViews(item)
            }

            private fun updateDriverViews(item: DailyPerformanceGroup) {
                val drivers = listOf(1L, 2L, 3L, 4L, 5L)
                val views = listOf(tvDriver1, tvDriver2, tvDriver3, tvDriver4, tvDriver5)

                drivers.forEachIndexed { index, driverId ->
                    val data = item.drivers[driverId]
                    views[index].text = if (data != null) {
                        "${data.services}\n${formatCurrency(data.fares)}"
                    } else {
                        "۰\n۰"
                    }
                }
            }
        }
    }
    private data class DailyPerformanceGroup(
        val date: String,
        val drivers: Map<Long, DailyPerformance>
    )
}