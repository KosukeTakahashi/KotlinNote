package jp.kosuke.kotlinnote

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.PermissionChecker
import android.util.Log
import kotlinx.android.synthetic.main.content_editor.*
import java.io.*
import java.nio.charset.Charset

/**
 * Created by Kosuke on 2017/11/07.
 */

enum class CharsetCodes(val charset: Charset) {
    UTF_8(Charsets.UTF_8),
    UTF_16(Charsets.UTF_16),
    UTF_16BE(Charsets.UTF_16BE),
    UTF_16LE(Charsets.UTF_16LE),
    UTF_32(Charsets.UTF_32),
    UTF_32BE(Charsets.UTF_32BE),
    UTF_32LE(Charsets.UTF_32LE),
    US_ASCII(Charsets.US_ASCII),
    ISO_8859_1(Charsets.ISO_8859_1),
    S_JIS(Charset.forName("Shift-JIS")),
    EUC_JP(Charset.forName("EUC_JP"))
}

class RequestCodes {
    companion object {
        val OPEN_FILE   = 16
        val CREATE_SAVE = 32
        val PERMISSION  = 64
    }
}

class Utils {
    companion object {
        fun read(act: Activity, context: Context, uri: Uri, charset: CharsetCodes): String {
            if (Build.VERSION_CODES.M <= Build.VERSION.SDK_INT && !hasPermission(context)) {
                requestStoragePermission(act, context)
                return ""
            }
            else {
                val temp = try {
                    act.contentResolver.openInputStream(uri)
                } catch (e: FileNotFoundException) {
                    val newfile = touch(File(Environment.getExternalStorageDirectory(), "KotlinNote/newfile.txt"))
                    val tmpUri = Uri.fromFile(newfile)
                    act.contentResolver.openInputStream(tmpUri)
                }

                val builder = StringBuilder()

                try {
                    val inStream = temp
                    val br = BufferedReader(InputStreamReader(
                            inStream, charset.charset))
                    var line = br.readLine()
                    while (line != null) {
                        builder.append(line).append("\n")
                        line = br.readLine()
                    }
                }
                catch (e: FileNotFoundException) {
                    Snackbar.make(act.editor, R.string.msg_file_not_found, Snackbar.LENGTH_SHORT).show()
                }
                catch (e: IOException) {
                    Snackbar.make(act.editor, R.string.msg_cant_open_file, Snackbar.LENGTH_SHORT).show()
                }

                return builder.toString()
            }
        }

        fun save(act: EditorActivity, context: Context, content: String, uri: Uri, charset: CharsetCodes) {
            if (Build.VERSION_CODES.M <= Build.VERSION.SDK_INT && !hasPermission(context)) {
                requestStoragePermission(act, context)
            }
            else {
                // なぜかフリーズ
//                val outStream = act.contentResolver.openOutputStream(uri)
//                val writer = outStream.writer(charset.charset)
//                writer.write(content)
//                writer.close()
//                outStream.close()
                // FileWriterとか使える？
                Log.d(act.appname, "Streams closed.")

            }
        }

        fun generateLineCounter(count: Int): String {
            var result = "1"
            for (i in 2..count) {
                result += "\n$i"
            }
            return result
        }

        @RequiresApi(Build.VERSION_CODES.M)
        fun requestStoragePermission(act: Activity, context: Context, showAlert: Boolean = false) {
            val permission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (showAlert) {
                AlertDialog
                        .Builder(context)
                        .setTitle(R.string.label_alertPermissionTitle)
                        .setMessage(R.string.label_alertPermission)
                        .setPositiveButton(R.string.label_grant, { _, _ ->
                            ActivityCompat.requestPermissions(act, permission, RequestCodes.PERMISSION)
                        })
                        .setNegativeButton(R.string.label_deny, null)
                        .create()
                        .show()
            }
            else {
                ActivityCompat.requestPermissions(act, permission,RequestCodes.PERMISSION)
            }
        }

        fun getPathFromUri(context: Context, uri: Uri): String {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                if ("com.android.externalstorage.documents" == uri.authority) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":")
                    val type = split[0]
                    if ("primary" == type)
                        return Environment.getExternalStorageDirectory().path + "/" + split[1]
                    else
                        return "/storage/" + type + "/" + split[1]
                }
                else if ("com.android.providers.downloads.documents" == uri.authority) {
                    val id = DocumentsContract.getDocumentId(uri)
                    val contentUri = ContentUris.withAppendedId(
                            Uri.parse("context://downloads/public_downloads"), id.toLong())
                    return getDataCol(context, contentUri, null, null)
                }
                else if ("com.android.providers.media.documents" == uri.authority) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":")
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(split[1])
                    val contentUri = MediaStore.Files.getContentUri("external")
                    return getDataCol(context, contentUri, selection, selectionArgs)
                }
            }
            else if ("content" == uri.scheme) {
                return getDataCol(context, uri, null, null)
            }
            else if ("file" == uri.scheme) {
                return uri.path
            }

            return ""
        }

        private fun touch(newfile: File): File {
            if (newfile.exists()) {
                if (newfile.isFile && newfile.canWrite())
                    return newfile
            }
            else {
                try {
                    Runtime.getRuntime().exec(arrayOf("mkdir", "-p", newfile.parent))
                    Runtime.getRuntime().exec(arrayOf("touch", newfile.absolutePath))
                }

                catch(e: InterruptedException) {
                    throw RuntimeException("InterruptException @ touch(File)")
                }
            }

            return newfile
        }

        private fun hasPermission(context: Context): Boolean {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                return true
            else
                return PermissionChecker.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        private fun getDataCol(context: Context, uri: Uri, selection: String?, selectionArgs: Array<String>?): String {
            val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
            val cursor = context.contentResolver.query(
                    uri, projection, selection, selectionArgs, null)

            cursor.use { cursor ->
                if (cursor.moveToFirst()) {
                    val cindex = cursor.getColumnIndexOrThrow(projection[0])
                    return cursor.getString(cindex)
                }
            }

            return ""
        }
    }
}