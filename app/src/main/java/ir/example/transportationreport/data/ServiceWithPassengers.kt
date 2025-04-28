package ir.example.transportationreport.data

import androidx.room.Embedded
import androidx.room.Relation
import ir.example.transportationreport.model.Passenger
import ir.example.transportationreport.model.Service

data class ServiceWithPassengers(
    @Embedded
    val service: Service,

    @Relation(
        parentColumn = "id",
        entityColumn = "serviceId"
    )
    val passengers: List<Passenger>
)