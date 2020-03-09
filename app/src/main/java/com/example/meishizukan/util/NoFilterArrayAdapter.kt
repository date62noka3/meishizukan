import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable

class NoFilterArrayAdapter(context: Context, layoutResource: Int, objects: MutableList<String>):
    ArrayAdapter<String>(context, layoutResource, objects),
    Filterable {

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun publishResults(charSequence: CharSequence?, filterResults: Filter.FilterResults) {
                notifyDataSetChanged()
            }

            override fun performFiltering(charSequence: CharSequence?): Filter.FilterResults {
                return FilterResults()
            }
        }
    }
}