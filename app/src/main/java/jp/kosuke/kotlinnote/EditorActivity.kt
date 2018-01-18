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
    private val appname = "jp.kosuke.KotlinNote"

    private lateinit var utils: Utils
    private var currentLineCount = 1

    lateinit var currentUri: Uri
    var currentCharset = CharsetCodes.UTF_8

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
        Log.d(appname, "onActivityResult called")
        if (requestCode == RequestCodes.OPEN_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Log.d(appname, "OPEN_FILE")

                currentUri = data.data
                val content = utils.read(currentUri, currentCharset)
                editor.setText(content)

                val lines = editor.lineCount
                val text = utils.generateLineCounter(lines)
                counter.text = text
                currentLineCount = lines

                Log.d(appname, "currentUri is $currentUri")
                toolbar.subtitle = utils.getPathFromUri(currentUri)
                Snackbar.make(editor, R.string.msg_file_opened, Snackbar.LENGTH_SHORT).show()
            }
            else {
                Log.e(appname, "data is null @ onActivityResult")
                Snackbar.make(editor, R.string.msg_cant_open_file, Snackbar.LENGTH_SHORT).show()
            }
        }
        else if (requestCode == RequestCodes.NAME_TO_SAVE && resultCode == Activity.RESULT_OK) {
            Log.d(appname, "NAME_TO_SAVE")
            if (data != null) {
                currentUri = data.data
                toolbar.subtitle = utils.getPathFromUri(currentUri)
                utils.save(editor.text.toString(), currentUri, currentCharset)
                Log.d(appname, "Saved @ onActivityResult : NAME_TO_SAVE")
            }
        }
        else {
            Log.d(appname, "else")
            Snackbar.make(editor, R.string.msg_canceled, Snackbar.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RequestCodes.PERMISSION && grantResults.isNotEmpty()) {Snackbar.make(editor, R.string.msg_permission_granted, Snackbar.LENGTH_SHORT).show()
            val content = utils.read(currentUri, currentCharset)
            editor.setText(content)
        }
        else {
            utils.requestStoragePermission()
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
            val text = utils.generateLineCounter(lines)
            counter.text = text
            currentLineCount = lines
        }
    }

    private fun init() {
        utils = Utils(this@EditorActivity, this@EditorActivity)

        val newfile = File(Environment.getExternalStorageDirectory(), "KotlinNote/newfile.txt")
        currentUri = Uri.fromFile(utils.touch(newfile))

        val path = utils.getPathFromUri(currentUri)
        toolbar.subtitle = path

        val action = intent.action
        val type = intent.type
        currentUri =
                if (Intent.ACTION_VIEW == action && type != null)
                    intent.data
                else
                    Uri.fromFile(
                            File(Environment.getExternalStorageDirectory(), "KotlinNote/newfile.txt")
                    )

        val content = utils.read(currentUri, currentCharset)
        editor.setText(content)

        val lines = editor.lineCount
        val text = utils.generateLineCounter(lines)
        counter.text = text
        currentLineCount = lines

        fab.setOnClickListener { view ->
            var saved = false

            Snackbar.make(view, R.string.msg_confirm_overwrite, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.label_name_to_save, { _ ->
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                        intent.addCategory(Intent.CATEGORY_OPENABLE)
                        intent.putExtra(Intent.EXTRA_TITLE, "untitled.txt")
                        intent.type = "text/*"
                        startActivityForResult(intent, RequestCodes.NAME_TO_SAVE)
                        saved = true
                    })
                    .addCallback(object: Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            if (event != DISMISS_EVENT_SWIPE && !saved) {
                                Log.d(appname, "content: ${editor.text.toString()}")
                                utils.save(editor.text.toString(), currentUri, currentCharset)
                                Snackbar.make(view, R.string.msg_saved, Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    })
                    .show()
        }

        editor.addTextChangedListener(this)

        open_file.setOnClickListener { _ ->
            // SAF 呼び出し
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            startActivityForResult(intent, RequestCodes.OPEN_FILE)
        }
    }

    // onActivityResult とかでやりたかった
    fun dialogCallback(charset: CharsetCodes) {
        Log.d(appname, "dialogCallback called :: charset = $charset")
        currentCharset = charset
        val content = utils.read(currentUri, currentCharset)
        editor.setText(content)

        val lines = editor.lineCount
        val text = utils.generateLineCounter(lines)
        counter.text = text
        currentLineCount = lines
    }
}
