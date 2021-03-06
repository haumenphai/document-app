package dotd.hmp.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ModelDao {
    @Insert
    fun insert(vararg model: Model)

    @Update
    fun update(vararg model: Model)

    @Delete
    fun delete(vararg model: Model)

    @Query("DELETE FROM model")
    fun deleteAll()

    @Query("SELECT * FROM model")
    fun getList(): List<Model>

    @Query("SELECT * FROM model")
    fun getLiveData(): LiveData<List<Model>>

    @Query("SELECT * FROM model WHERE id=:id")
    fun getModel(id: Int): Model



}
