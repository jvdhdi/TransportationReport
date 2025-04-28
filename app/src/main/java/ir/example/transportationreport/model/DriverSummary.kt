package ir.example.transportationreport.model

data class DriverSummary(
    val driverId: Long,
    val driverName: String,
    val totalServices: Int,
    val totalFares: Int
)