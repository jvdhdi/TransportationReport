package ir.example.transportationreport.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ir.hamsaa.persiandatepicker.api.PersianPickerDate
import ir.hamsaa.persiandatepicker.api.PersianPickerListener
import ir.example.transportationreport.databinding.ActivityDateSelectionBinding

class DateSelectionActivity : AppCompatActivity(), PersianPickerListener {

    private lateinit var binding: ActivityDateSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDateSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etDate.setOnClickListener {
            ShamsiDatePicker.show(this, this)
        }
    }

    override fun onDateSelected(persianPickerDate: PersianPickerDate) {
        val dayNumber = persianPickerDate.dayOfWeek
        val dayName = when (dayNumber) {
            0 -> "شنبه"
            1 -> "یکشنبه"
            2 -> "دوشنبه"
            3 -> "سه‌شنبه"
            4 -> "چهارشنبه"
            5 -> "پنجشنبه"
            6 -> "جمعه"
            else -> ""
        }

        val formattedDate = "%04d/%02d/%02d".format(
            persianPickerDate.persianYear,
            persianPickerDate.persianMonth,
            persianPickerDate.persianDay
        )

        binding.etDate.setText(formattedDate)
        showConfirmationDialog(dayName, formattedDate)
    }

    private fun showConfirmationDialog(dayName: String, date: String) {
        AlertDialog.Builder(this)
            .setTitle("تأیید تاریخ")
            .setMessage("آیا میخواهید سرویس های روز $dayName $date را ثبت نمائید؟")
            .setPositiveButton("بله") { _, _ ->
                navigateToAddService(dayName, date)
            }
            .setNegativeButton("خیر", null)
            .show()
    }

    private fun navigateToAddService(dayName: String, date: String) {
        try {
            val intent = Intent(this, AddServiceActivity::class.java).apply {
                putExtra("DAY_NAME", dayName)
                putExtra("FORMATTED_DATE", date)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "خطا در انتقال به صفحه بعد", Toast.LENGTH_SHORT).show()
            Log.e("NavigationError", "خطا در انتقال به صفحه بعد", e)
        }
    }

    override fun onDismissed() {
        // Handle dismiss if needed
    }
}