package jp.kosuke.kotlinnote

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.annotation.RequiresApi
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_editor.*
import kotlinx.android.synthetic.main.content_editor.*
import java.io.File

class EditorActivity : AppCompatActivity(), TextWatcher {
    val appname = "jp.kosuke.KotlinNote"
    var currentCharset = CharsetCodes.UTF_8
    var currentUri =
            Uri.fromFile(File(
                    Environment.getExternalStorageDirectory(), "KotlinNote/newfile.txt"))

    private var currentLineCount = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)
        setSupportActionBar(toolbar)
        init()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.itemId == R.id.action_choose_encoding) {
            //ダイアログ出す
            val dlg = SingleSelectDialogFragment()
            dlg.show(fragmentManager, "encode-selection")
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCodes.OPEN_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                currentUri = data.data
                val content = Utils.read(this@EditorActivity, this@EditorActivity, currentUri, currentCharset)
                toolbar.subtitle = Utils.getPathFromUri(this@EditorActivity, currentUri)
                Snackbar.make(editor, R.string.msg_file_opened, Snackbar.LENGTH_SHORT).show()
            }
            else {
                Log.e(appname, "data is null @ onActivityResult")
                Snackbar.make(editor, R.string.msg_cant_open_file, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RequestCodes.PERMISSION && grantResults.isNotEmpty()) {
            Utils.requestStoragePermission(this@EditorActivity, this@EditorActivity)
        }
        else {
            Snackbar.make(editor, R.string.msg_permission_granted, Snackbar.LENGTH_SHORT).show()
            val content = Utils.read(this, this, currentUri, currentCharset)
            editor.setText(content)
        }
    }

    override fun afterTextChanged(p0: Editable?) {
        // 空実装
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // 空実装
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        val lines = editor.lineCount
        if (lines != currentLineCount) {
            val text = Utils.generateLineCounter(lines)
            counter.text = text
            currentLineCount = lines
        }
    }

    private fun init() {
        val path = Utils.getPathFromUri(this, currentUri)
        toolbar.subtitle = path

        fab.setOnClickListener { view ->
            Snackbar.make(view, R.string.msg_confirm_overwrite, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.label_name_to_save, { _ ->
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                        intent.addCategory(Intent.CATEGORY_OPENABLE)
                        intent.putExtra(Intent.EXTRA_TITLE, "untitled.txt")
                        intent.type = "text/*"
                        startActivityForResult(intent, RequestCodes.CREATE_SAVE)
                    })
                    .addCallback(object: Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            if (event != DISMISS_EVENT_SWIPE) {
                                Log.d(appname, "content: ${editor.text.toString()}")
                                Utils.save(this@EditorActivity, this@EditorActivity, editor.text.toString(), currentUri, currentCharset)
                                Snackbar.make(view, R.string.msg_saved, Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    })
                    .show()
        }

        editor.addTextChangedListener(this)

        open_file.setOnClickListener { view ->
            // SAF 呼び出し
        }
    }
}
