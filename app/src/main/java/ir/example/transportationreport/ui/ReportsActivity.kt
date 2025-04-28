package ir.example.transportationreport.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ir.example.transportationreport.databinding.ActivityReportsBinding

class ReportsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReportsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGeneralReport.setOnClickListener {
            startActivity(Intent(this, ReportCollectionActivity::class.java))
        }

        binding.btnDriverReport.setOnClickListener {
            startActivity(Intent(this, DriverPerformanceActivity::class.java))
        }
    }
}