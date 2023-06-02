package io.github.kichikuou.xsystem4

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.system.Os
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import java.io.File
import java.nio.charset.Charset

private data class Item(val name: String, val path: File, val homedir: File, val icon: File?, val error: String?) {
    companion object {
        fun fromDirectory(dir: File, homedir: File, context: Context): Item {
            var ini = File(dir, "System40.ini")
            if (!ini.exists())
                ini = File(dir, "AliceStart.ini")
            if (!ini.exists()) {
                val err = context.getString(R.string.toast_no_ini, dir.path)
                Log.w("Launcher", err)
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
            Log.w("Launcher", err)
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
}

private class GameListAdapter(activity: Activity) : BaseAdapter() {
    private val items: ArrayList<Item> = arrayListOf()
    private val context: Context = activity

    init {
        for (storagePath in activity.getExternalFilesDirs(null)) {
            val state = Environment.getExternalStorageState(storagePath)
            Log.i("launcher", "${storagePath}: $state")
            if (state != Environment.MEDIA_MOUNTED && state != Environment.MEDIA_MOUNTED_READ_ONLY) {
                continue
            }
            val homedir = File(storagePath, ".xsystem4")
            val files = storagePath.listFiles() ?: continue
            for (path in files) {
                if (!path.equals(homedir) && path.isDirectory) {
                    items.add(Item.fromDirectory(path, homedir, activity))
                }
            }
            if (state == Environment.MEDIA_MOUNTED && files.isEmpty()) {
                // Create a dummy file and do a media scan so that `storagePath` is discoverable via MTP.
                // https://issuetracker.google.com/issues/37071807
                val dummyFile = File(storagePath, "dummy.txt")
                dummyFile.writeText("Create a subfolder here that stores game files.")
                MediaScannerConnection.scanFile(activity, arrayOf(dummyFile.absolutePath), null, null)
            }
            // Make sure that save directories are group-readable (so that they can be transferred
            // via MTP), while old xsystem4 created them with permission 0700.
            // TODO: Remove this after some transition period.
            if (homedir.exists()) {
                makeGroupReadable(homedir)
            }
        }
    }

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): Any = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val item = items[position]
        val view = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.launcher_item, parent, false)
        if (item.icon != null) {
            val bmp = BitmapFactory.decodeFile(item.icon.path)
            view.findViewById<ImageView>(R.id.icon).setImageBitmap(bmp)
        }
        val title = view.findViewById<TextView>(R.id.title)
        title.text = item.name
        if (item.error != null) {
            title.setTextColor(Color.GRAY)
        }
        return view
    }
}

class LauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.launcher)

        val listView = findViewById<ListView>(R.id.list)
        listView.emptyView = findViewById(R.id.empty)
        findViewById<TextView>(R.id.usage).text = Html.fromHtml(getString(R.string.usage))
        listView.setOnItemClickListener { _, _, pos, _ ->
            launchGame(listView.adapter.getItem(pos) as Item)
        }
        updateGameList()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.launcher_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.refresh -> {
                updateGameList()
                true
            }
            R.id.help -> {
                AlertDialog.Builder(this)
                    .setMessage(Html.fromHtml(getString(R.string.usage)))
                    .show()
                true
            }
            R.id.licenses -> {
                val intent = Intent(this, LicensesMenuActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateGameList() {
        val listView = findViewById<ListView>(R.id.list)
        listView.adapter = GameListAdapter(this)
    }

    private fun launchGame(item: Item) {
        if (item.error != null) {
            AlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setMessage(item.error)
                .setPositiveButton("OK", null)
                .show()
            return
        }
        val i = Intent()
        i.setClass(applicationContext, XSystem4Activity::class.java)
        i.putExtra(XSystem4Activity.EXTRA_GAME_ROOT, item.path.path)
        i.putExtra(XSystem4Activity.EXTRA_XSYSTEM4_HOME, item.homedir.path)
        startActivity(i)
    }
}

fun makeGroupReadable(dir: File) {
    if (Os.stat(dir.path).st_mode shr 3 and 4 != 0) {
        return  // Already group-readable.
    }
    Log.i("Launcher", "Copying ${dir.path} to make it group-readable")
    val tmpDir = File(dir.parent, ".xsystem4_temp")
    dir.copyRecursively(tmpDir, true)
    dir.deleteRecursively()
    tmpDir.renameTo(dir)
}
