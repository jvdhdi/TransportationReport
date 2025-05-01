package ir.example.transportationreport.ui

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.core.text.BidiFormatter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.fontbox.cmap.CMapParser
import ir.example.transportationreport.ui.ReportCollectionActivity.ReportItem
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class PdfExporter(private val context: Context) {

    private val bidiFormatter = BidiFormatter.getInstance(Locale("fa", "IR"))
    private lateinit var persianFont: PDType0Font
    private var currentYPosition = 0f
    private val pageMargin = 40f
    private val rowHeight = 25f
    private val columnWidths = floatArrayOf(100f, 100f, 100f, 100f, 100f, 80f)
    private val a4Width = PDRectangle.A4.width



    fun exportToPdf(items: List<ReportItem>) {
        val document = PDDocument()
        try {
            val cmapParser = CMapParser()
            val identityH = context.assets.open("cmap/Identity-H").use {
                cmapParser.parse(it)
            }


            val fontStream = context.assets.open("fonts/Vazir.ttf")
            persianFont = PDType0Font.load(document, fontStream, true) // پارامتر سوم = embedSubset

            val page = PDPage(PDRectangle.A4)
            document.addPage(page)

            PDPageContentStream(document, page).use { contentStream ->
                currentYPosition = page.mediaBox.height - pageMargin
                contentStream.setFont(persianFont, 12f)

                // هدر اصلی
                addCenteredHeader(contentStream, "گزارش سرویس های حمل و نقل", 18f)

                // افزودن محتوا
                items.forEach { item ->
                    when (item) {
                        is ReportItem.OverallHeader -> addOverallSection(contentStream, item)
                        is ReportItem.DailyHeader -> addDailySection(contentStream, item)
                        is ReportItem.ServiceRow -> addServiceRow(contentStream, item)
                        ReportItem.Separator -> addSeparator()
                    }
                }
            }

            // ذخیره فایل PDF
            val outputFile = getOutputFile()
            document.save(outputFile)
            Log.d("PDF Export", "PDF با موفقیت ایجاد شد: ${outputFile.absolutePath}")

        } catch (e: Exception) {
            Log.e("PDF Export", "خطا در ایجاد PDF", e)
            throw RuntimeException("خطا در ایجاد PDF", e)
        } finally {
            document.close()
        }
    }

    private fun addCenteredHeader(
        contentStream: PDPageContentStream,
        text: String,
        fontSize: Float
    ) {
        contentStream.beginText()
        contentStream.setFont(persianFont, fontSize)
        val textWidth = persianFont.getStringWidth(text) * fontSize / 1000f
        val xPosition = (a4Width - textWidth) / 2

        contentStream.newLineAtOffset(xPosition, currentYPosition)
        contentStream.showText(bidiFormatter.unicodeWrap(text))
        contentStream.endText()
        currentYPosition -= rowHeight * 2
    }

    private fun addOverallSection(
        contentStream: PDPageContentStream,
        item: ReportItem.OverallHeader
    ) {
        val text = "تعداد کل سرویس ها: ${fixNumbers(item.totalServices.toString())} | جمع کل: ${formatCurrency(item.grandTotal)}"
        addTextLine(contentStream, text, 14f)
    }

    private fun addDailySection(
        contentStream: PDPageContentStream,
        item: ReportItem.DailyHeader
    ) {
        val text = "${fixTextDirection(item.date)} | جمع روزانه: ${formatCurrency(item.dailyTotal)}"
        addTextLine(contentStream, text, 12f)
    }

    private fun addServiceRow(
        contentStream: PDPageContentStream,
        item: ReportItem.ServiceRow
    ) {
        contentStream.beginText()
        val columns = listOf(
            item.col1, item.col2, item.col3, item.col4, item.col5, item.total
        )

        var currentX = pageMargin
        columns.forEachIndexed { index, text ->
            contentStream.setFont(persianFont, if (item.isHeader) 11f else 10f)
            val rtlText = bidiFormatter.unicodeWrap(text)
            val xPosition = a4Width - currentX - columnWidths[index]

            contentStream.newLineAtOffset(xPosition, currentYPosition)
            contentStream.showText(rtlText)
            contentStream.newLineAtOffset(-xPosition, 0f)
            currentX += columnWidths[index]
        }
        contentStream.endText()
        currentYPosition -= rowHeight
    }

    private fun addTextLine(
        contentStream: PDPageContentStream,
        text: String,
        fontSize: Float
    ) {
        contentStream.beginText()
        contentStream.setFont(persianFont, fontSize)
        val textWidth = persianFont.getStringWidth(text) * fontSize / 1000f
        val xPosition = (a4Width - textWidth - pageMargin)

        contentStream.newLineAtOffset(xPosition, currentYPosition)
        contentStream.showText(text)
        contentStream.endText()
        currentYPosition -= rowHeight
    }

    private fun addSeparator() {
        currentYPosition -= 10f
    }

    private fun formatCurrency(amount: Int): String {
        val formatter = NumberFormat.getNumberInstance(Locale("fa", "IR"))
        return "${formatter.format(amount)} تومان"
    }

    private fun fixNumbers(text: String): String {
        return text.map {
            when (it) {
                '0' -> '۰'
                '1' -> '۱'
                '2' -> '۲'
                '3' -> '۳'
                '4' -> '۴'
                '5' -> '۵'
                '6' -> '۶'
                '7' -> '۷'
                '8' -> '۸'
                '9' -> '۹'
                else -> it
            }
        }.joinToString("")
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