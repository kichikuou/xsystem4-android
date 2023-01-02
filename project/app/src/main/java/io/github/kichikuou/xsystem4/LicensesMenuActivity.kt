package io.github.kichikuou.xsystem4

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import android.widget.SimpleAdapter

class LicensesMenuActivity : Activity() {
    class Entry(val displayName: String, val fileName: String, val url: String)
    private val entries: ArrayList<Entry> = arrayListOf(
        Entry("xsystem4", "xsystem4", "https://github.com/nunuhara/xsystem4"),
        Entry("libsys4", "libsys4", "https://github.com/nunuhara/libsys4"),
        Entry("SDL", "SDL", "https://www.libsdl.org/"),
        Entry("cglm", "cglm", "https://github.com/recp/cglm"),
        Entry("libFLAC", "flac", "https://xiph.org/flac/"),
        Entry("FreeType", "freetype", "https://freetype.org/"),
        Entry("libffi", "libffi", "https://sourceware.org/libffi/"),
        Entry("libjpeg-turbo", "libjpeg-turbo", "https://libjpeg-turbo.org/"),
        Entry("libogg", "libogg", "https://xiph.org/ogg/"),
        Entry("libpng", "libpng", "http://www.libpng.org/pub/png/libpng.html"),
        Entry("libsndfile", "libsndfile", "https://libsndfile.github.io/libsndfile/"),
        Entry("libvorbis", "libvorbis", "https://xiph.org/vorbis/"),
        Entry("libwebp", "libwebp", "https://chromium.googlesource.com/webm/libwebp/"),
        Entry("libopus", "opus", "https://opus-codec.org/downloads/"),
        Entry("VL Gothic Regular", "VL-Gothic-Regular", "http://vlgothic.dicey.org/"),
        Entry("Hanazono Mincho A", "HanaMinA", "http://fonts.jp/hanazono/"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_licenses_menu)

        val items = entries.map { mapOf("name" to it.displayName, "url" to it.url)}
        val listView = findViewById<ListView>(R.id.list)
        listView.adapter = SimpleAdapter(this, items, android.R.layout.simple_list_item_2,
            arrayOf("name", "url"), intArrayOf(android.R.id.text1, android.R.id.text2))
        listView.setOnItemClickListener { _, _, pos, _ ->
            val intent = Intent(this, LicensesActivity::class.java).apply {
                putExtra(LicensesActivity.EXTRA_DISPLAY_NAME, entries[pos].displayName)
                putExtra(LicensesActivity.EXTRA_FILE_NAME, entries[pos].fileName)
            }
            startActivity(intent)
        }
    }
}