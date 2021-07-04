package dotd.hmp.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.Toast
import dotd.hmp.R
import dotd.hmp.data.NO_ICON
import dotd.hmp.databinding.DialogAddNewModelBinding
import dotd.hmp.hepler.UIHelper

class DialogAddNewModel(val context: Context) {
    private val view by lazy { LayoutInflater.from(context).inflate(R.layout.dialog_add_new_model, null) }
    val b by lazy { DialogAddNewModelBinding.bind(view) }
    val dialog by lazy { Dialog(context) }

    init {
        dialog.setContentView(b.root)
        dialog.setCanceledOnTouchOutside(false)
    }

    fun setBtnOkClick(callBack: (modelName: String, icon: Int) -> Unit) {
        var icon = NO_ICON

        b.btnOk.setOnClickListener {
            val text = b.edittext.text.toString()
            if (text.trim().isEmpty()) {
                AlertDialog.Builder(context).setMessage("Model name mustn't be empty!")
                return@setOnClickListener
            }
            callBack(text, icon)
            cancel()
        }
        b.imgIcon.setOnClickListener {
            UIHelper.hideKeyboardFrom(context, b.edittext)
            DialogPickIcon(context).apply {
                setItemIconClick { itemIcon ->
                    icon = itemIcon.iconResource
                    cancel()
                    this@DialogAddNewModel.b.imgIcon.setImageResource(icon)
                }
                show()
            }
        }

    }

    fun setBtnCancelClick(callBack: () -> Unit) {
        b.btnCancel.setOnClickListener {
            callBack()
            cancel()
        }
    }

    fun cancel() = dialog.cancel()
    fun show() = dialog.show()

}