package io.github.kichikuou.xsystem4

import android.app.Activity
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
import java.io.File

class LauncherActivity : Activity() {
    data class Entry(val path: File, val name: String)
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
            val files = storagePath.listFiles() ?: continue
            for (path in files) {
                if (path.isDirectory) {
                    games.add(Entry(path, path.name))
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

    private fun launchGame(entry: Entry) {
        val i = Intent()
        i.setClass(applicationContext, XSystem4Activity::class.java)
        i.putExtra(XSystem4Activity.EXTRA_GAME_ROOT, entry.path.path)
        startActivity(i)
    }
}