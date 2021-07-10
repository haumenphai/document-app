package dotd.hmp.data

import android.graphics.Color
import android.util.Log
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dotd.hmp.R
import java.io.Serializable
import java.lang.Exception
import java.util.*


/*
jsonData:
[
   {        <====== This is Record
       "id": {     <====== This is Field
            "fieldType": "TEXT",
            "value": "abZhdhDZMWlamKzmeDZZ"
       },
       "createTime": {
            "fieldType": "DATETIME",
            "value": "12031200222"
       },
       "name": {
            "fieldType": "TEXT",
            "value": "Peter"
        },
        "age": {
            "fieldType": "NUMBER",
            "value": "20"
        }
    },
]
All field value store as String,
Not use org.json.JSONObject
 */

@Entity
class Model: Serializable {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
    var name: String = ""
    var icon: Int? = null
    var jsonFields: String = ""
    var jsonData: String = "[]"
    var sequence: Int = 0
    var description: String = ""

    @Ignore
    var isSelected = false

    constructor()

    @Ignore
    constructor(name: String, icon: Int) {
        this.name = name
        this.icon = icon
    }

    companion object {
        val itemAddNewModel by lazy {
                Model("Add", R.drawable.ic_baseline_add_24).apply {
                    description = "add new model"
            }
        }
    }
    fun isItemAddNewModel(): Boolean = this == itemAddNewModel


    fun setFieldList(list: MutableList<Field>) {
        list.forEach { field ->
            if (field.fieldName.isDefaultField()) {
                var defaultFieldName = ""
                defaultField.forEach { defaultFieldName += "$it, " }
                throw Exception("Field name mustn't be the same as default field name: (${field.fieldName}), default field: [$defaultFieldName]")
            }
        }
        this.jsonFields = Gson().toJson(list)
    }

    // field list not contain default field
    fun getFieldList(): List<Field> = Gson().fromJson(this.jsonFields, Array<Field>::class.java).asList()

    fun addRecord(record: JsonObject) {
        if (!isRecordValidate(record)) {
            throw Exception("Exception when add new record, jsonOject not enough field when compared to fieldList of Model")
        }
        insertDefaultField(record)
        val recordList = getRecordList()
        recordList.add(record)
        jsonData = recordList.toString()
    }

    fun deleteRecord(record: JsonObject) {
        val id = record.getValueOfField("id")
        val recordList = getRecordList()
        for (r in recordList) {
            val id2 = r.getValueOfField("id")
            if (id == id2) {
                recordList.remove(r)
                jsonData = recordList.toString()
                break
            }
        }
    }

    fun updateRecord(record: JsonObject) {
        if (!isRecordValidate(record)) {
            throw Exception("Exception when update new record, jsonOject not enough field when compared to fieldList of Model")
        }

        val id = record.getValueOfField("id")
        val recordList = getRecordList()

        for ((i, v) in recordList.withIndex()) {
            val id2 = v.getValueOfField("id")
            if (id == id2) {
                insertUpdatetime(record)
                recordList.set(i, record)

                jsonData = recordList.toString()
                break
            }
        }
    }

    fun deleteAllRecord() {
        jsonData = "[]"
    }

    fun getRecordList(): MutableList<JsonObject> {
        return Gson().fromJson(jsonData, Array<JsonObject>::class.java).asList().toMutableList()
    }

    fun setRecordList(list: List<JsonObject>) {
        this.jsonData = Gson().toJson(list)
    }

    fun isRecordValidate(record: JsonObject): Boolean {
        getFieldList().forEach {
            // record does not contain field defined in model
            if (!record.has(it.fieldName)) {
                return false
                // not check field has [fieldType, value] because performance
            }
        }
        return true
    }

    fun sortByField(fieldName: String): Model {
        val recordList = getRecordList()
        if (recordList.isEmpty()) return this
        if (!hasField(fieldName)) {
            Log.e("Model", "Error at: [Model.kt, sortByField()]: Can't sort records, " +
                      "field name: \"$fieldName\" doesn't not exist.")
            return this
        }
        getFieldList().forEach {
            if (!it.isFieldCanSorted()) {
                Log.e("Model", "Error at: [Model.kt, sortByField()]: $it is not field type can sort.")
                return this
            }
        }

        val model = this.clone()
        val listSorted = recordList.sortedWith(compareBy(
            {it.getValueOfField(fieldName)},
            {it.getValueOfField(fieldName)}
        ))
        model.jsonData = listSorted.toString()
        return model
    }

    fun hasField(fieldName: String): Boolean {
        getFieldList().forEach {
            if (it.fieldName == fieldName)
                return true
        }
        return false
    }

    fun hasIcon(): Boolean = icon != null

    fun clone(): Model {
        val model = Model()
        model.id = this.id
        model.name = this.name
        model.icon = this.icon
        model.jsonFields = this.jsonFields
        model.jsonData = this.jsonData
        model.sequence = this.sequence
        model.description = this.description
        return model
    }


    private fun insertDefaultField(record: JsonObject) {
        val id = JsonObject()
        id.addProperty("fieldType", FieldType.TEXT.toString())
        id.addProperty("value", UUID.randomUUID().toString())

        val createTime = JsonObject()
        createTime.addProperty("fieldType", FieldType.DATETIME.toString())
        createTime.addProperty("value", System.currentTimeMillis().toString())

        record.add("id", id)
        record.add("create_time", createTime)
        record.add("update_time", createTime)
    }

    private fun insertUpdatetime(record: JsonObject) {
        val updateTime = JsonObject()
        updateTime.addProperty("fieldType", FieldType.DATETIME.toString())
        updateTime.addProperty("value", System.currentTimeMillis().toString())
        record.add("update_time", updateTime)
    }
}

enum class FieldType {
    TEXT, NUMBER, DATETIME
}

class Field(var fieldName: String, var fieldType: FieldType) {

    fun isValid(): Boolean = fieldName.trim() != ""

    fun isFieldCanSorted(): Boolean {
        return this.fieldType in arrayOf(
            FieldType.TEXT,
            FieldType.NUMBER,
            FieldType.DATETIME
        )
    }

    override fun toString(): String = "($fieldName: $fieldType)"

}

val defaultField = listOf("id", "create_time", "update_time")
fun String.isDefaultField() = this in defaultField

fun JsonObject.getValueOfField(fieldName: String): String {
    return this.get(fieldName).asJsonObject.get("value").asString
}

fun JsonObject.getFieldType(fieldName: String): String {
    return this.get(fieldName).asJsonObject.get("fieldType").asString
}

fun JsonObject.updateFieldValue(fieldName: String, value: String): JsonObject {
    val jsonObj = this.deepCopy()
    jsonObj[fieldName].asJsonObject.addProperty("value", value)
    return jsonObj
}
