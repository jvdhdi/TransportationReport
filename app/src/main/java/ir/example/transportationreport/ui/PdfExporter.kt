package ir.example.transportationreport.ui

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.core.text.BidiFormatter
import com.itextpdf.io.font.FontProgramFactory
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.*
import ir.example.transportationreport.ui.ReportCollectionActivity.ReportItem
import java.io.File
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class PdfExporter(private val context: Context) {

    private lateinit var persianFont: PdfFont
    private val bidiFormatter = BidiFormatter.getInstance(Locale("fa", "IR"))

    init {
        initializeFonts()
    }

    private fun initializeFonts() {
        try {
            val fontBytes = context.assets.open("fonts/B_Nazanin.TTF").readBytes()
            val fontProgram = FontProgramFactory.createFont(fontBytes)
            persianFont = PdfFontFactory.createFont(
                fontProgram,
                PdfEncodings.IDENTITY_H,
                PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED
            )
        } catch (e: IOException) {
            Log.e("PDF Font", "Error loading font", e)
            throw RuntimeException("Error loading font", e)
        }
    }

    fun exportToPdf(items: List<ReportItem>) {
        var document: Document? = null
        try {
            val outputFile = getOutputFile()
            val writer = PdfWriter(outputFile)
            val pdfDoc = PdfDocument(writer).apply {
                setTagged()
            }

            document = Document(pdfDoc, PageSize.A4.rotate()).apply {
                setTextAlignment(TextAlignment.LEFT)
                setProperty(Property.BASE_DIRECTION, BaseDirection.LEFT_TO_RIGHT)
                setMargins(40f, 40f, 40f, 40f)
                setFont(persianFont)
                addHeader()
                addContent(items)
            }

            Log.d("PDF Export", "PDF file created: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("PDF Export", "Error creating PDF", e)
            throw RuntimeException("Error creating PDF", e)
        } finally {
            document?.close()
        }
    }

    private fun Document.addHeader() {
        add(
            createPersianParagraph("گزارش سرویس های حمل و نقل")
                .setFontSize(18f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20f)
        )
    }

    private fun Document.addContent(items: List<ReportItem>) {
        items.forEach { item ->
            when (item) {
                is ReportItem.OverallHeader -> addOverallSection(item)
                is ReportItem.DailyHeader -> addDailySection(item)
                is ReportItem.ServiceRow -> addServiceRow(item)
                ReportItem.Separator -> addSeparator()
            }
        }
    }

    private fun Document.addOverallSection(item: ReportItem.OverallHeader) {
        add(
            createPersianParagraph("تعداد کل سرویس ها: ${fixTextDirection(item.totalServices.toString())} | جمع کل: ${formatCurrency(item.grandTotal)}")
                .setFontSize(14f)
                .setMarginBottom(15f)
        )
    }

    private fun Document.addDailySection(item: ReportItem.DailyHeader) {
        add(
            createPersianParagraph("${fixTextDirection(item.date)} | جمع روزانه: ${formatCurrency(item.dailyTotal)}")
                .setFontSize(12f)
                .setMarginBottom(10f)
        )
    }

    private fun Document.addServiceRow(item: ReportItem.ServiceRow) {
        val table = Table(6).apply {
            width = UnitValue.createPercentValue(100f)
            setHorizontalAlignment(HorizontalAlignment.LEFT)
            setTextAlignment(TextAlignment.LEFT)
            setProperty(Property.BASE_DIRECTION, BaseDirection.RIGHT_TO_LEFT)
        }

        if (item.total == "کرایه") {
            addHeaderRow(table, item)
        } else {
            addDataRow(table, item)
        }

        add(table)
    }

    private fun addHeaderRow(table: Table, item: ReportItem.ServiceRow) {
        table.apply {
            addCell(createCell(fixTextDirection(item.col1), true))
            addCell(createCell(fixTextDirection(item.col2), true))
            addCell(createCell(fixTextDirection(item.col3), true))
            addCell(createCell(fixTextDirection(item.col4), true))
            addCell(createCell(fixTextDirection(item.col5), true))
            addCell(createCell(fixTextDirection(item.total), true))
        }
    }

    private fun addDataRow(table: Table, item: ReportItem.ServiceRow) {
        table.apply {
            addCell(createCell(fixTextDirection(item.col1), false))
            addCell(createCell(fixTextDirection(item.col2), false))
            addCell(createCell(fixTextDirection(item.col3), false))
            addCell(createCell(fixTextDirection(item.col4), false))
            addCell(createCell(fixTextDirection(item.col5), false))
            addCell(createCell(fixTextDirection(item.total), false))
        }
    }

    private fun createCell(text: String, isHeader: Boolean): Cell {
        return Cell().apply {
            width = UnitValue.createPercentValue(20f)
            add(createPersianParagraph(text).apply {
                if (isHeader) {
                    setBold()
                    setFontSize(11f)
                } else {
                    setFontSize(10f)
                }
                setProperty(Property.BASE_DIRECTION, BaseDirection.RIGHT_TO_LEFT)
                setTextAlignment(TextAlignment.RIGHT)
            })
            setPadding(5f)
            setVerticalAlignment(VerticalAlignment.MIDDLE)
            setTextAlignment(TextAlignment.RIGHT)
            setProperty(Property.BASE_DIRECTION, BaseDirection.RIGHT_TO_LEFT)
            if (isHeader) {
                setBackgroundColor(DeviceRgb(240, 240, 240))
            }
        }
    }

    private fun createPersianParagraph(text: String): Paragraph {
        return Paragraph(bidiFormatter.unicodeWrap(text)).apply {
            setFont(persianFont)
            setTextAlignment(TextAlignment.LEFT)
            setProperty(Property.BASE_DIRECTION, BaseDirection.RIGHT_TO_LEFT)
            setTextAlignment(TextAlignment.RIGHT)
        }
    }

    private fun Document.addSeparator() {
        add(Paragraph("\n"))
    }

    private fun formatCurrency(amount: Int): String {
        val formatter = NumberFormat.getNumberInstance(Locale("fa", "IR"))
        return "${formatter.format(amount)} تومان"
    }

    private fun fixTextDirection(text: String): String {
        return bidiFormatter.unicodeWrap(text)
    }

    private fun getOutputFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "TransportationReports/TransportReport_$timeStamp.pdf"
        ).apply { parentFile?.mkdirs() }
    }
}
