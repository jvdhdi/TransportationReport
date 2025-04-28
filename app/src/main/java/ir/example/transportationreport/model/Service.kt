// Service.kt
package ir.example.transportationreport.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "services")
data class Service(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val driverId: Long,
    val date: String,
    val totalFare: Int
)