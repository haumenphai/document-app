package dotd.hmp.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dotd.hmp.R
import dotd.hmp.data.FieldType
import dotd.hmp.data.Model
import dotd.hmp.databinding.ItemDataModelBinding
import dotd.hmp.hepler.setTextHTML

class DataModelAdpater():
        RecyclerView.Adapter<DataModelAdpater.DataModelHolder>() {

    private lateinit var jsonArr: JsonArray
    private lateinit var model: Model

    fun setModel(model: Model) {
        this.model = model
        this.jsonArr = model.getJsonArray()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataModelHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_data_model, parent, false)
        return DataModelHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: DataModelHolder, position: Int) {
        val b = holder.b
        val jsonObject = jsonArr[position].asJsonObject
        b.tvSequence.text = "#${position+1}"

        var text = ""
        jsonObject.keySet().forEach { fieldName ->
            val fieldType = jsonObject.get(fieldName).asJsonObject.get("fieldType").asString
            val value =  jsonObject.get(fieldName).asJsonObject.get("value").asString
            if (fieldType == FieldType.TEXT.toString() ||
                fieldType == FieldType.NUMBER.toString()) {
                text += """
                    <font color="black">
                           <b>${fieldName}:</b>
                    </font> $value <br/>
                """.trimIndent()
            }
        }
        b.tvData.setTextHTML(text)
    }

    override fun getItemCount(): Int = jsonArr.size()

    lateinit var onClickItem: (jsonObj: JsonObject) -> Unit
    inner class DataModelHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val b = ItemDataModelBinding.bind(itemView)
        init {
            b.background.setOnClickListener {
                onClickItem(jsonArr[layoutPosition].asJsonObject)
            }
        }
    }

}