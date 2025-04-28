package ir.example.transportationreport.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.properties.BaseDirection
import com.itextpdf.layout.properties.TextAlignment
import java.io.File
import java.io.FileOutputStream

class SimplePdfExporterprivate (val context: Context) {

    fun createTestPdf() {
        var pdfDocument: PdfDocument? = null
        var document: Document? = null
        var font: PdfFont? = null

        try {
            // 1. بارگذاری فونت فارسی
            font = PdfFontFactory.createFont(
                context.assets.open("fonts/Vazir.ttf"),
                PdfEncodings.IDENTITY_H,
                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
            )

            // 2. تنظیم مسیر ذخیره‌سازی
            val outputFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Persian_Report.pdf"
            )

            // 3. ایجاد سند PDF
            val writer = PdfWriter(FileOutputStream(outputFile))
            pdfDocument = PdfDocument(writer)
            document = Document(pdfDocument, PageSize.A4)
            document.setMargins(50f, 50f, 50f, 50f)

            // 4. ایجاد محتوای فارسی
            val content = listOf(
                "عنوان گزارش: گزارش روزانه حمل و نقل",
                "تاریخ: ${toPersianDigits("1403/05/15")}",
                "تعداد سرویس‌ها: ${toPersianDigits("25")}",
                "مجموع درآمد: ${toPersianDigits("12,500,000")} تومان",
                "این یک متن تستی با اعداد فارسی ۱۲۳۴۵۶ است."
            )

            // 5. افزودن محتوا به سند
            content.forEach { text ->
                val paragraph = Paragraph()
                    .add("\u202B$text\u202C") // کاراکترهای کنترل RTL
                    .setFont(font)
                    .setFontSize(14f)
                    .setBaseDirection(BaseDirection.RIGHT_TO_LEFT)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setFixedLeading(20f)

                document.add(paragraph)
            }

            Toast.makeText(context, "PDF با موفقیت ایجاد شد", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("PDF_ERROR", "خطا: ${e.stackTraceToString()}")
            Toast.makeText(context, "خطا در ایجاد PDF: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            document?.close()
            pdfDocument?.close()
        }
    }

    private fun toPersianDigits(input: String): String {
        val latinDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        return input.map { c ->
            val index = latinDigits.indexOf(c)
            if (index != -1) persianDigits[index] else c
        }.joinToString("")
    }
}