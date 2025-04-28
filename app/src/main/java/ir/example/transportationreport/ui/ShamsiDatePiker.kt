package ir.example.transportationreport.ui

import android.content.Context
import ir.hamsaa.persiandatepicker.PersianDatePickerDialog
import ir.hamsaa.persiandatepicker.api.PersianPickerListener

object ShamsiDatePicker {

    fun show(
        context: Context,
        listener: PersianPickerListener
    ) {
        PersianDatePickerDialog(context)
            .setListener(listener)
            .show()
    }
}