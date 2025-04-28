// Passenger.kt
package ir.example.transportationreport.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "passengers",
    foreignKeys = [ForeignKey(
        entity = Service::class,
        parentColumns = ["id"],
        childColumns = ["serviceId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("serviceId")]
)
data class Passenger(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val destination: String,
    val fare: Int,
    val serviceId: Long
)