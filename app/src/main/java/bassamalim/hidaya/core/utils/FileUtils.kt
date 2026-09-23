package bassamalim.hidaya.core.utils

import android.content.Context
import android.widget.Toast
import bassamalim.hidaya.R
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object FileUtils {

    fun createDir(context: Context, path: String): Boolean {
        val dir = File(context.getExternalFilesDir(null).toString() + path)

        return if (!dir.exists()) dir.mkdirs() else false
    }

    fun deleteFile(context: Context, path: String): Boolean {
        val file = File(context.getExternalFilesDir(null).toString() + path)
        return if (file.exists()) file.deleteRecursively() else false
    }

    fun deleteDirRecursive(target: File) {
        if (target.isDirectory) {
            for (child in target.listFiles()!!) deleteDirRecursive(child)
        }
        if (target.isDirectory) target.delete()
    }

    fun getJsonFromAssets(context: Context, fileName: String?): String? {
        val jsonString: String = try {
            val `is` = context.assets.open(fileName!!)

            val size = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()

            String(buffer, StandardCharsets.UTF_8)
        } catch (e: IOException) {
            e.report()
            e.printStackTrace()
            return null
        }

        return jsonString
    }

    fun getJsonFromDownloads(path: String): String {
        var jsonStr = ""

        var fin: FileInputStream? = null
        try {
            val file = File(path)
            fin = FileInputStream(file)

            val fc = fin.channel
            val bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())

            jsonStr = Charset.defaultCharset().decode(bb).toString()
        } catch (e: Exception) {
            e.report()
            e.printStackTrace()
        } finally {
            try {
                fin?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return jsonStr
    }

    fun showWaitMassage(context: Context) {
        Toast.makeText(
            context, context.getString(R.string.wait_for_download), Toast.LENGTH_SHORT
        ).show()
    }

}