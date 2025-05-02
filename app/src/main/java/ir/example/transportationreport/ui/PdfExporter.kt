package ir.example.transportationreport.ui

import android.content.Context
import android.os.Environment
import android.util.Log
import com.ibm.icu.text.Bidi
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import ir.example.transportationreport.ui.ReportCollectionActivity.ReportItem
import java.io.File
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class PdfExporter(private val context: Context) {

    private lateinit var persianFont: PDType0Font
    private var currentYPosition = 0f
    private val pageMargin = 40f
    private val rowHeight = 25f
    private val columnWidths = floatArrayOf(100f, 100f, 100f, 100f, 100f, 80f)
    private val a4Width = PDRectangle.A4.width
    private var currentPage: PDPage? = null
    private var contentStream: PDPageContentStream? = null

    fun exportToPdf(items: List<ReportItem>) {
        val document = PDDocument()
        try {
            System.setProperty("cmap.base.url", "file:///android_asset/cmap/")

            val fontStream = context.assets.open("fonts/Vazir.ttf")
            persianFont = PDType0Font.load(document, fontStream, true)

            currentPage = PDPage(PDRectangle.A4)
            document.addPage(currentPage)
            contentStream = PDPageContentStream(document, currentPage)
            currentYPosition = currentPage!!.mediaBox.height - pageMargin

            addCenteredHeader("گزارش سرویس های حمل و نقل", 18f, document)

            items.forEach { item ->
                checkPageSpace(50f, document)
                when (item) {
                    is ReportItem.OverallHeader -> addOverallSection(item)
                    is ReportItem.DailyHeader -> addDailySection(item)
                    is ReportItem.ServiceRow -> addServiceRow(item)
                    ReportItem.Separator -> addSeparator()
                }
            }

            contentStream?.close()
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

    private fun addCenteredHeader(text: String, fontSize: Float, document: PDDocument) {
        checkPageSpace(rowHeight * 2, document)
        contentStream?.beginText()
        contentStream?.setFont(persianFont, fontSize)

        val rtlText = reorderRtlText(text)
        val textWidth = persianFont.getStringWidth(rtlText) * fontSize / 1000f
        val xPosition = (a4Width - textWidth) / 2

        contentStream?.newLineAtOffset(xPosition, currentYPosition)
        contentStream?.showText(rtlText)
        contentStream?.endText()
        currentYPosition -= rowHeight * 2
    }

    private fun addOverallSection(item: ReportItem.OverallHeader) {
        val text = "تعداد کل سرویس ها: ${formatNumber(item.totalServices)} | جمع کل: ${formatCurrency(item.grandTotal)}"
        addTextLine(text, 14f)
    }

    private fun addDailySection(item: ReportItem.DailyHeader) {
        val text = "${reorderRtlText(item.date)} | جمع روزانه: ${formatCurrency(item.dailyTotal)}"
        addTextLine(text, 12f)
    }

    private fun addServiceRow(item: ReportItem.ServiceRow) {
        contentStream?.beginText()
        val columns = listOf(
            item.col1, item.col2, item.col3, item.col4, item.col5, item.total
        )

        var cumulativeWidth = 0f
        columns.reversed().forEachIndexed { index, text ->
            val columnIndex = columns.size - 1 - index
            val rtlText = reorderRtlText(text)
            val fontSize = if (item.isHeader) 11f else 10f

            // محاسبه موقعیت X از راست صفحه
            val xPosition = a4Width - pageMargin - cumulativeWidth - (columnWidths[columnIndex] / 2)
            val textWidth = persianFont.getStringWidth(rtlText) * fontSize / 1000f
            val adjustedX = xPosition - (textWidth / 2)

            contentStream?.setFont(persianFont, fontSize)
            contentStream?.newLineAtOffset(adjustedX, currentYPosition)
            contentStream?.showText(rtlText)
            contentStream?.newLineAtOffset(-adjustedX, 0f)

            cumulativeWidth += columnWidths[columnIndex]
        }
        contentStream?.endText()
        currentYPosition -= rowHeight
    }

    private fun addTextLine(text: String, fontSize: Float) {
        contentStream?.beginText()
        contentStream?.setFont(persianFont, fontSize)

        val rtlText = reorderRtlText(text)
        val textWidth = persianFont.getStringWidth(rtlText) * fontSize / 1000f
        val xPosition = a4Width - textWidth - pageMargin

        contentStream?.newLineAtOffset(xPosition, currentYPosition)
        contentStream?.showText(rtlText)
        contentStream?.endText()
        currentYPosition -= rowHeight
    }

    private fun addSeparator() {
        checkPageSpace(20f, null)
        contentStream?.setLineWidth(0.5f)
        contentStream?.moveTo(pageMargin, currentYPosition)
        contentStream?.lineTo(a4Width - pageMargin, currentYPosition)
        contentStream?.stroke()
        currentYPosition -= 15f
    }

    private fun checkPageSpace(requiredHeight: Float, document: PDDocument?) {
        if (currentYPosition - requiredHeight < pageMargin) {
            contentStream?.close()
            currentPage = PDPage(PDRectangle.A4)
            document?.addPage(currentPage)
            contentStream = PDPageContentStream(document, currentPage)
            currentYPosition = currentPage!!.mediaBox.height - pageMargin
        }
    }

    private fun reorderRtlText(text: String): String {
        return try {
            val bidi = Bidi(text, Bidi.DIRECTION_RIGHT_TO_LEFT)
            bidi.writeReordered(Bidi.DO_MIRRORING.toInt())
        } catch (e: Exception) {
            text
        }
    }

    private fun formatCurrency(amount: Int): String {
        val formatter = NumberFormat.getNumberInstance(Locale("fa", "IR"))
        return "${formatter.format(amount)} تومان"
    }

    private fun formatNumber(number: Int): String {
        val formatter = NumberFormat.getNumberInstance(Locale("fa", "IR"))
        return formatter.format(number)
    }

    @Throws(IOException::class)
    private fun getOutputFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "TransportationReports/TransportReport_$timeStamp.pdf"
        ).apply { parentFile?.mkdirs() }
    }
}