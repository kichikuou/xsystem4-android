package io.github.kichikuou.xsystem4

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.nio.charset.Charset

class LauncherActivity : Activity() {
    data class Entry(val name: String, val path: File, val homedir: File)
    private val games: ArrayList<Entry> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.launcher)

        val listView = findViewById<ListView>(R.id.list)
        listView.emptyView = findViewById(R.id.empty)
        findViewById<TextView>(R.id.usage).text = Html.fromHtml(getString(R.string.usage))
        listView.setOnItemClickListener { _, _, pos, _ ->
            launchGame(games[pos])
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
        games.clear()
        for (storagePath in getExternalFilesDirs(null)) {
            val state = Environment.getExternalStorageState(storagePath)
            Log.i("launcher", "${storagePath}: $state")
            if (state != Environment.MEDIA_MOUNTED && state != Environment.MEDIA_MOUNTED_READ_ONLY) {
                continue
            }
            val homedir = File(storagePath, ".xsystem4")
            val files = storagePath.listFiles() ?: continue
            for (path in files) {
                if (!path.equals(homedir) && path.isDirectory) {
                    val title = getGameTitle(path)
                    games.add(Entry(title, path, homedir))
                }
            }
            if (state == Environment.MEDIA_MOUNTED && files.isEmpty()) {
                // Create a dummy file and do a media scan so that `storagePath` is discoverable via MTP.
                // https://issuetracker.google.com/issues/37071807
                val dummyFile = File(storagePath, "dummy.txt")
                dummyFile.writeText("Create a subfolder here that stores game files.")
                MediaScannerConnection.scanFile(this, arrayOf(dummyFile.absolutePath), null, null)
            }
        }
        val items = games.map(Entry::name).toMutableList()
        val listView = findViewById<ListView>(R.id.list)
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
    }

    private fun getGameTitle(dir: File): String {
        var ini = File(dir, "System40.ini")
        if (!ini.exists())
            ini = File(dir, "AliceStart.ini")
        if (!ini.exists()) {
            val err = getString(R.string.toast_no_ini, dir.path)
            Log.w("Launcher", err)
            Toast.makeText(this, err, Toast.LENGTH_LONG).show()
            return dir.name
        }
        val regex = Regex("""GameName\s*=\s*"(.*)"""")
        for (line in ini.readLines(Charset.forName("Shift_JIS"))) {
            regex.matchEntire(line)?.let {
                return it.groupValues[1]
            }
        }
        val err = getString(R.string.toast_no_GameName, ini.path)
        Log.w("Launcher", err)
        Toast.makeText(this, err, Toast.LENGTH_LONG).show()
        return dir.name
    }

    private fun launchGame(entry: Entry) {
        val i = Intent()
        i.setClass(applicationContext, XSystem4Activity::class.java)
        i.putExtra(XSystem4Activity.EXTRA_GAME_ROOT, entry.path.path)
        i.putExtra(XSystem4Activity.EXTRA_XSYSTEM4_HOME, entry.homedir.path)
        startActivity(i)
    }
}