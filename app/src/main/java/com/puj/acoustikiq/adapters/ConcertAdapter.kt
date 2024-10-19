import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.puj.acoustikiq.adapters.VenueAdapter
import com.puj.acoustikiq.databinding.ItemConcertBinding
import com.puj.acoustikiq.model.Concert
import java.text.SimpleDateFormat
import java.util.Locale

class ConcertAdapter(
    private val concerts: List<Concert>,
    private val onConcertClick: (Concert) -> Unit
) : RecyclerView.Adapter<ConcertAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemConcertBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConcertBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val concert = concerts[position]
        holder.binding.concertNameTextView.text = "Concert: ${concert.name}"

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.binding.concertDateTextView.text = "Date: ${dateFormat.format(concert.date)}"

        holder.binding.concertLocationTextView.text = "Location: ${concert.location.latitude}, ${concert.location.longitude}"

        val venueAdapter = VenueAdapter(concert.venues) { venue ->

            Toast.makeText(holder.itemView.context, "Clicked on ${venue.name}", Toast.LENGTH_SHORT).show()
        }

        holder.binding.venueRecyclerView.layoutManager = LinearLayoutManager(holder.binding.root.context)
        holder.binding.venueRecyclerView.adapter = venueAdapter

        holder.itemView.setOnClickListener {
            onConcertClick(concert)
        }
    }

    override fun getItemCount(): Int = concerts.size
}