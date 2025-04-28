package ir.example.transportationreport.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "days")
data class Day(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val totalServices: Int
)