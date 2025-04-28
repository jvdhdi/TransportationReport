package ir.example.transportationreport.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ir.example.transportationreport.R
import ir.example.transportationreport.data.ServiceDao.DailyPerformance
import java.text.NumberFormat
import java.util.Locale

class DailyPerformanceAdapter : RecyclerView.Adapter<DailyPerformanceAdapter.ViewHolder>() {

    private var items = listOf<DailyPerformanceGroup>()

    fun submitList(newItems: List<DailyPerformanceGroup>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_daily_performance, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvDriver1: TextView = itemView.findViewById(R.id.tv_driver1)
        private val tvDriver2: TextView = itemView.findViewById(R.id.tv_driver2)
        private val tvDriver3: TextView = itemView.findViewById(R.id.tv_driver3)
        private val tvDriver4: TextView = itemView.findViewById(R.id.tv_driver4)
        private val tvDriver5: TextView = itemView.findViewById(R.id.tv_driver5)

        fun bind(item: DailyPerformanceGroup) {
            tvDate.text = item.date

            fun getDriverData(driverId: Long): Pair<Int, Int> {
                val data = item.drivers[driverId]
                return data?.let { it.services to it.fares } ?: 0 to 0
            }

            fun formatText(services: Int, fares: Int): String {
                return "${services}\n${formatCurrency(fares)}"
            }

            val (services1, fares1) = getDriverData(1)
            val (services2, fares2) = getDriverData(2)
            val (services3, fares3) = getDriverData(3)
            val (services4, fares4) = getDriverData(4)
            val (services5, fares5) = getDriverData(5)

            tvDriver1.text = formatText(services1, fares1)
            tvDriver2.text = formatText(services2, fares2)
            tvDriver3.text = formatText(services3, fares3)
            tvDriver4.text = formatText(services4, fares4)
            tvDriver5.text = formatText(services5, fares5)
        }

        private fun formatCurrency(amount: Int): String {
            return NumberFormat.getNumberInstance(Locale.US).format(amount)
        }
    }

    data class DailyPerformanceGroup(
        val date: String,
        val drivers: Map<Long, DailyPerformance>
    )
}