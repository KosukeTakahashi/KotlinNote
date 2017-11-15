package jp.kosuke.kotlinnote

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log

/**
 * Created by kousu on 2017/05/20.
 */
class SingleSelectDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        var chosen = CharsetCodes.UTF_8
        val act = activity as EditorActivity
        val initial = act.currentCharset
        val builder = AlertDialog.Builder(act)
        builder.setTitle(R.string.label_chooseEncodingTitle)
                .setSingleChoiceItems(R.array.encodes, getId(initial), DialogInterface.OnClickListener(
                        { _, which -> chosen = getCharsetCodes(which) }))
                .setPositiveButton(R.string.label_OK, {_, which ->
                    val charset = getCharsetCodes(which)
                    act.dialogCallback(charset)
                })

        return builder.create()
    }

    private fun getId(csc: CharsetCodes): Int {
        return when (csc) {
            CharsetCodes.UTF_8      -> 0
            CharsetCodes.UTF_16     -> 1
            CharsetCodes.UTF_16BE   -> 2
            CharsetCodes.UTF_16LE   -> 3
            CharsetCodes.UTF_32     -> 4
            CharsetCodes.UTF_32BE   -> 5
            CharsetCodes.UTF_32LE   -> 6
            CharsetCodes.US_ASCII   -> 7
            CharsetCodes.ISO_8859_1 -> 8
            CharsetCodes.S_JIS      -> 9
            CharsetCodes.EUC_JP     -> 10
            else -> 0
        }
    }

    private fun getCharsetCodes(charsetId: Int): CharsetCodes {
        return when (charsetId) {
            0 -> CharsetCodes.UTF_8
            1 -> CharsetCodes.UTF_16
            2 -> CharsetCodes.UTF_16BE
            3 -> CharsetCodes.UTF_16LE
            4 -> CharsetCodes.UTF_32
            5 -> CharsetCodes.UTF_32BE
            6 -> CharsetCodes.UTF_32LE
            7 -> CharsetCodes.US_ASCII
            8 -> CharsetCodes.ISO_8859_1
            9 -> CharsetCodes.S_JIS
            10 -> CharsetCodes.EUC_JP
            else -> CharsetCodes.UTF_8
        }
    }
}