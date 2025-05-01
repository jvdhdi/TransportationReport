package ir.example.transportationreport.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
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
import java.text.NumberFormat
import java.util.Calendar
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverPerformanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
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

    private fun updateDriverUI(tvServices: TextView, tvFares: TextView, summary: DriverSummary) {
        tvServices.text = summary.totalServices.toString()
        tvFares.text = formatCurrency(summary.totalFares)
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
        val document = PDDocument()
        try {
            // بارگذاری فونت‌های فارسی
            val font = PDType0Font.load(document, assets.open("fonts/Vazir.ttf"))
            val boldFont = PDType0Font.load(document, assets.open("fonts/Vazir_Bold.ttf"))

            // ایجاد صفحه جدید با جهت افقی (Landscape)
            val page = PDPage(PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width))
            document.addPage(page)
            val mediaBox = page.mediaBox

            PDPageContentStream(document, page).use { contentStream ->
                // محاسبه موقعیت اولیه
                var yPosition = mediaBox.height - 40f
                val marginX = 40f

                // هدر اصلی
                contentStream.setFont(boldFont, 18f)
                drawCenteredText(
                    contentStream = contentStream,
                    text = "گزارش عملکرد رانندگان",
                    centerX = mediaBox.width / 2,
                    y = yPosition,
                    font = boldFont,
                    fontSize = 18f
                )
                yPosition -= 40f

                // تاریخ گزارش
                contentStream.setFont(font, 12f)
                drawTextRtl(
                    contentStream = contentStream,
                    text = "تاریخ گزارش: ${getCurrentDate()}",
                    rightMargin = marginX,
                    y = yPosition,
                    font = font,
                    fontSize = 12f
                )
                yPosition -= 60f

                // جمع کل ماهانه
                addMonthlySummary(
                    contentStream = contentStream,
                    marginX = marginX,
                    yStart = yPosition,
                    normalFont = font,
                    boldFont = boldFont
                )
                yPosition -= 100f

                // آمار رانندگان
                addDriverSummary(
                    contentStream = contentStream,
                    marginX = marginX,
                    yStart = yPosition,
                    normalFont = font,
                    boldFont = boldFont
                )
                yPosition -= 200f

                // عملکرد روزانه
                addDailyPerformance(
                    contentStream = contentStream,
                    marginX = marginX,
                    yStart = yPosition,
                    normalFont = font,
                    boldFont = boldFont
                )
            }

            // ذخیره فایل
            val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val fileName = "DriverReport_${System.currentTimeMillis()}.pdf"
            val outputFile = File(outputDir, fileName)
            document.save(FileOutputStream(outputFile))

            Toast.makeText(
                this,
                "فایل PDF با موفقیت ذخیره شد:\n${outputFile.absolutePath}",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Log.e("PDF_EXPORT", "خطا در تولید PDF", e)
            Toast.makeText(
                this,
                "خطا در ایجاد فایل PDF: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        } finally {
            document.close()
        }
    }

    private fun drawCenteredText(
        contentStream: PDPageContentStream,
        text: String,
        centerX: Float,
        y: Float,
        font: PDType0Font,
        fontSize: Float
    ) {
        val textWidth = font.getStringWidth(text) / 1000f * fontSize
        val startX = centerX - (textWidth / 2)

        contentStream.beginText()
        contentStream.setFont(font, fontSize)
        contentStream.newLineAtOffset(startX, y)
        contentStream.showText(text)
        contentStream.endText()
    }

    private fun drawTextRtl(
        contentStream: PDPageContentStream,
        text: String,
        rightMargin: Float,
        y: Float,
        font: PDType0Font,
        fontSize: Float
    ) {
        val textWidth = font.getStringWidth(text) / 1000f * fontSize
        val pageWidth = PDRectangle.A4.height // چون صفحه افقی است
        val x = pageWidth - rightMargin - textWidth

        contentStream.beginText()
        contentStream.setFont(font, fontSize)
        contentStream.newLineAtOffset(x, y)
        contentStream.showText(text)
        contentStream.endText()
    }

    private fun addMonthlySummary(
        contentStream: PDPageContentStream,
        marginX: Float,
        yStart: Float,
        normalFont: PDType0Font,
        boldFont: PDType0Font
    ) {
        viewModel.getMonthlySummary().value?.let { summary ->
            var y = yStart

            contentStream.setFont(boldFont, 16f)
            drawTextRtl(
                contentStream = contentStream,
                text = "جمع کل ماهانه",
                rightMargin = marginX,
                y = y,
                font = boldFont,
                fontSize = 16f
            )
            y -= 30f

            contentStream.setFont(normalFont, 14f)
            drawTextRtl(
                contentStream = contentStream,
                text = "تعداد کل سرویس‌ها: ${formatNumber(summary.totalServices)}",
                rightMargin = marginX,
                y = y,
                font = normalFont,
                fontSize = 14f
            )
            y -= 25f

            drawTextRtl(
                contentStream = contentStream,
                text = "مجموع درآمد کل: ${formatCurrency(summary.totalFares)} تومان",
                rightMargin = marginX,
                y = y,
                font = normalFont,
                fontSize = 14f
            )
        }
    }

    private fun addDriverSummary(
        contentStream: PDPageContentStream,
        marginX: Float,
        yStart: Float,
        normalFont: PDType0Font,
        boldFont: PDType0Font
    ) {
        viewModel.getDriverPerformanceReport().value?.let { summaries ->
            var y = yStart

            contentStream.setFont(boldFont, 16f)
            drawTextRtl(
                contentStream = contentStream,
                text = "آمار عملکرد رانندگان",
                rightMargin = marginX,
                y = y,
                font = boldFont,
                fontSize = 16f
            )
            y -= 30f

            contentStream.setFont(normalFont, 14f)
            summaries.forEach { summary ->
                drawTextRtl(
                    contentStream = contentStream,
                    text = "${driverMap[summary.driverId] ?: "نامشخص"}: ${summary.totalServices} سرویس - ${formatCurrency(summary.totalFares)}",
                    rightMargin = marginX,
                    y = y,
                    font = normalFont,
                    fontSize = 14f
                )
                y -= 25f
            }
        }
    }

    private fun addDailyPerformance(
        contentStream: PDPageContentStream,
        marginX: Float,
        yStart: Float,
        normalFont: PDType0Font,
        boldFont: PDType0Font
    ) {
        val dailyData = dailyAdapter.getItems()
        if (dailyData.isEmpty()) return

        var y = yStart

        contentStream.setFont(boldFont, 16f)
        drawTextRtl(
            contentStream = contentStream,
            text = "عملکرد روزانه",
            rightMargin = marginX,
            y = y,
            font = boldFont,
            fontSize = 16f
        )
        y -= 30f

        contentStream.setFont(normalFont, 12f)
        dailyData.forEach { group ->
            drawTextRtl(
                contentStream = contentStream,
                text = group.date,
                rightMargin = marginX,
                y = y,
                font = normalFont,
                fontSize = 12f
            )
            y -= 20f

            driverMap.keys.sorted().forEach { driverId ->
                val data = group.drivers[driverId]
                val text = if (data != null) {
                    "${driverMap[driverId] ?: "نامشخص"}: ${data.services} سرویس - ${formatCurrency(data.fares)}"
                } else {
                    "${driverMap[driverId] ?: "نامشخص"}: ۰ سرویس - ۰ تومان"
                }
                drawTextRtl(
                    contentStream = contentStream,
                    text = text,
                    rightMargin = marginX + 20f,
                    y = y,
                    font = normalFont,
                    fontSize = 12f
                )
                y -= 20f
            }
            y -= 10f
        }
    }

    private fun formatCurrency(amount: Int): String {
        val formatter = NumberFormat.getNumberInstance(Locale("fa", "IR"))
        return formatter.format(amount).replace(',', '٫')
    }

    private fun formatNumber(number: Int): String {
        val formatter = NumberFormat.getNumberInstance(Locale("fa", "IR"))
        return formatter.format(number).replace(',', '٫')
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}/${
            (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')}/${
            calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')}"
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