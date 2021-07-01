package dotd.hmp.data

import android.graphics.Color
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
class Model {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
    var name: String = ""
    var color: Int = 0
    var jsonFields: String = ""
    var sequence: Int = 0
    var description = ""

    constructor()

    @Ignore
    constructor(name: String, color: Int) {
        this.name = name
        this.color = color
    }

    @Ignore
    constructor(name: String, color: Int, jsonFileds: String) {
        this.name = name
        this.color = color
        this.jsonFields = jsonFileds
    }

    companion object {
        val itemAddNewModel by lazy {
                Model("Add", Color.RED).apply {
                description = "add new model"
            }
        }
    }

}