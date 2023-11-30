package io.github.kichikuou.xsystem4

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.system.Os
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

interface GameListObserver {
    fun onInstallProgress(path: String)
    fun onInstallSuccess()
    fun onInstallFailure(msgId: Int)
}

data class Item(val name: String, val path: File, val homedir: File, val icon: File?, val error: String?) {
    companion object {
        fun fromDirectory(dir: File, homedir: File, context: Context): Item {
            var ini = File(dir, "System40.ini")
            if (!ini.exists())
                ini = File(dir, "AliceStart.ini")
            if (!ini.exists()) {
                val err = context.getString(R.string.toast_no_ini, dir.path)
                Log.w("GameList", err)
                return Item(dir.name, dir, homedir, null, err)
            }
            val icon = findIcon(dir)
            val regex = Regex("""GameName\s*=\s*"(.*)"""")
            for (line in ini.readLines(Charset.forName("Shift_JIS"))) {
                regex.matchEntire(line)?.let {
                    return Item(it.groupValues[1], dir, homedir, icon, null)
                }
            }
            val err = context.getString(R.string.toast_no_GameName, ini.path)
            Log.w("GameList", err)
            return Item(dir.name, dir, homedir, icon, err)
        }

        private fun findIcon(dir: File): File? {
            dir.listFiles()?.forEach {
                if (it.extension == "ico") { return it }
            }
            dir.listFiles { file -> file.extension == "exe" }?.forEach { exeFile ->
                PEResourceExtractor.create(exeFile)?.extractIcon()?.let { bytes ->
                    val f = File(dir, ".xsystem4.ico")
                    f.writeBytes(bytes)
                    return f
                }
            }
            return null
        }
    }

    fun getIconBitmap(reqSize: Int): Bitmap? {
        val bytes = icon?.readBytes() ?: return null
        val buf = ByteBuffer.wrap(bytes)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        buf.skip(4)
        val numIcons = buf.short.toInt()

        // If the icon contains multiple images, convert it to an icon containing only the image
        // closest to the requested size.
        if (numIcons > 1) {
            val bestMatchEntry = Array(numIcons) {
                val entryBytes = ByteArray(16)
                buf.get(entryBytes)
                entryBytes
            }.minWith(compareBy({
                // Compare the sizes first,
                val width = if (it[0].toInt() == 0) 256 else it[0].toInt()
                kotlin.math.abs(width - reqSize)
            }, {
                // then pick the one with the highest number of bits per pixel.
                -it[6]
            }))
            buf.position(4)
            buf.putShort(1)
            buf.put(bestMatchEntry)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}

class GameList(activity: Activity) {
    private val items: ArrayList<Item> = arrayListOf()
    private val storageDirs: ArrayList<File> = arrayListOf()
    operator fun get(index: Int): Item = items[index]
    val size: Int get() = items.size
    var observer: GameListObserver? = null
    var isInstalling = false
        private set

    class InstallFailureException(val msgId: Int) : Exception()

    init {
        for (storagePath in activity.getExternalFilesDirs(null).filterNotNull()) {
            val state = Environment.getExternalStorageState(storagePath)
            Log.i("GameList", "${storagePath}: $state")
            if (state != Environment.MEDIA_MOUNTED && state != Environment.MEDIA_MOUNTED_READ_ONLY) {
                continue
            }
            storageDirs.add(storagePath)
            val homedir = File(storagePath, ".xsystem4")
            val files = storagePath.listFiles() ?: continue
            for (path in files) {
                if (!path.name.startsWith(".") && path.isDirectory) {
                    items.add(Item.fromDirectory(path, homedir, activity))
                }
            }
            if (state == Environment.MEDIA_MOUNTED && files.isEmpty()) {
                // Create a dummy file and do a media scan so that `storagePath` is discoverable via MTP.
                // https://issuetracker.google.com/issues/37071807
                val dummyFile = File(storagePath, "dummy.txt")
                dummyFile.writeText("Create a subfolder here that stores game files.")
                MediaScannerConnection.scanFile(
                    activity,
                    arrayOf(dummyFile.absolutePath),
                    null,
                    null
                )
            }
            // Make sure that save directories are group-readable (so that they can be transferred
            // via MTP), while old xsystem4 created them with permission 0700.
            // TODO: Remove this after some transition period.
            if (homedir.exists()) {
                makeGroupReadable(homedir)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun install(input: InputStream, activity: Activity) {
        isInstalling = true
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val storagePath = storageDirs[0]
                val path = withContext(Dispatchers.IO) {
                    doInstall(input, storagePath) { msg ->
                        GlobalScope.launch(Dispatchers.Main) {
                            observer?.onInstallProgress(msg)
                        }
                    }
                }
                val homedir = File(storagePath, ".xsystem4")
                items.add(Item.fromDirectory(path, homedir, activity))
                observer?.onInstallSuccess()
            } catch (e: InstallFailureException) {
                observer?.onInstallFailure(e.msgId)
            } catch (e: Exception) {
                Log.e("launcher", "Failed to extract ZIP", e)
                observer?.onInstallFailure(R.string.zip_extraction_error)
            }
            isInstalling = false
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun doInstall(input: InputStream, storagePath: File, progressCallback: (String) -> Unit): File {
        val tempDir = File(storagePath, ".install_temp")
        tempDir.deleteRecursively()
        val zip = ZipInputStream(input.buffered(), Charset.forName("Shift_JIS"))
        var gameRoot: File? = null
        zip.use {
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) {
                    continue
                }
                val path = File(tempDir, entry.name)
                Log.i("GameList", "Extracting ${entry.name}")
                progressCallback(entry.name)
                path.parentFile?.mkdirs()
                path.outputStream().buffered().use { zip.copyTo(it) }

                if (path.name == "System40.ini" || path.name == "AliceStart.ini") {
                    gameRoot = path.parentFile
                }
            }
        }
        val srcDir = gameRoot ?: throw InstallFailureException(R.string.zip_no_ini)
        var dstDir = File(storagePath, srcDir.name)
        var i = 1
        while (dstDir.exists()) {
            dstDir = File(storagePath, "${srcDir.name} (${i++})")
        }
        srcDir.renameTo(dstDir)
        tempDir.deleteRecursively()
        return dstDir
    }

    fun uninstall(item: Item) {
        item.path.deleteRecursively()
        items.remove(item)
    }
}

private fun makeGroupReadable(dir: File) {
    if (Os.stat(dir.path).st_mode shr 3 and 4 != 0) {
        return  // Already group-readable.
    }
    Log.i("Launcher", "Copying ${dir.path} to make it group-readable")
    val tmpDir = File(dir.parent, ".xsystem4_temp")
    dir.copyRecursively(tmpDir, true)
    dir.deleteRecursively()
    tmpDir.renameTo(dir)
}
