package dotd.hmp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dotd.hmp.databinding.ItemModelBinding

class ModelApdater(private var list: List<Model> = mutableListOf()): RecyclerView.Adapter<ModelApdater.ModelHolder>() {

    @JvmName("setList1")
    fun setList(list: List<Model>) {
        this.list = list
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_model, parent, false)
        return ModelHolder(view)
    }

    override fun onBindViewHolder(holder: ModelHolder, position: Int) {
        val b = holder.b
        val model = list[position]

        b.image.setBackgroundColor(model.color)
        b.modelName.text = model.name
    }

    override fun getItemCount(): Int = list.size


    lateinit var onClickItem: (model: Model) -> Unit

    inner class ModelHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val b: ItemModelBinding = ItemModelBinding.bind(itemView)
        init {
            b.background.setOnClickListener {
                onClickItem(list[layoutPosition])
            }
        }
    }

}