package io.github.kichikuou.xsystem4

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView

private const val ICON_SIZE_DP = 40
private const val INSTALL_REQUEST = 1
private const val STATE_PROGRESS_TEXT = "progressText"

private class GameListAdapter(activity: Activity, refresh: Boolean) : BaseAdapter() {
    companion object {
        private var savedGameList: GameList? = null
    }
    private val context: Context = activity
    val gameList: GameList

    init {
        if (savedGameList == null || refresh) {
            savedGameList = GameList(activity)
        }
        gameList = savedGameList!!
    }

    override fun getCount(): Int = gameList.size
    override fun getItem(position: Int): Any = gameList[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val item = gameList[position]
        val view = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.launcher_item, parent, false)
        val icon = item.getIconBitmap((ICON_SIZE_DP * context.resources.displayMetrics.density).toInt())
        view.findViewById<ImageView>(R.id.icon).setImageBitmap(icon)
        val title = view.findViewById<TextView>(R.id.title)
        title.text = item.name
        title.setTextColor(if (item.error != null) Color.GRAY else Color.BLACK)
        return view
    }
}

class LauncherActivity : Activity(), GameListObserver {
    private lateinit var adapter: GameListAdapter
    private var progressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.launcher)

        adapter = GameListAdapter(this, false)
        adapter.gameList.observer = this
        if (adapter.gameList.isInstalling) {
            showProgressDialog(savedInstanceState)
        }

        val listView = findViewById<ListView>(R.id.list)
        listView.adapter = adapter
        listView.emptyView = findViewById(R.id.empty)
        findViewById<TextView>(R.id.usage).text = Html.fromHtml(getString(R.string.usage))
        listView.setOnItemClickListener { _, _, pos, _ ->
            onListItemClick(listView.adapter.getItem(pos) as Item)
        }
        listView.setOnItemLongClickListener { _, _, pos, _ ->
            onItemLongClick(listView.adapter.getItem(pos) as Item)
        }
    }

    override fun onDestroy() {
        adapter.gameList.observer = null
        dismissProgressDialog()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        progressDialog?.let {
            outState.putCharSequence(STATE_PROGRESS_TEXT, it.findViewById<TextView>(R.id.text).text)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.launcher_menu, menu)
        // The charset parameter of ZipInputStream is not supported on Android <7.0.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            menu.findItem(R.id.install_from_zip).isEnabled = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.refresh -> {
                adapter = GameListAdapter(this, true)
                findViewById<ListView>(R.id.list).adapter = adapter
                true
            }
            R.id.install_from_zip -> {
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.type = "application/zip"
                startActivityForResult(Intent.createChooser(i, getString(R.string.choose_a_file)), INSTALL_REQUEST)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK)
            return
        val uri = data?.data ?: return
        when (requestCode) {
            INSTALL_REQUEST -> {
                val input = contentResolver.openInputStream(uri) ?: return
                showProgressDialog()
                adapter.gameList.install(input, this)
            }
        }
    }

    override fun onInstallProgress(path: String) {
        progressDialog?.findViewById<TextView>(R.id.text)?.text = getString(R.string.install_progress, path)
    }

    override fun onInstallSuccess() {
        dismissProgressDialog()
        adapter.notifyDataSetChanged()
    }

    override fun onInstallFailure(msgId: Int) {
        dismissProgressDialog()
        AlertDialog.Builder(this).setTitle(R.string.error)
            .setMessage(msgId)
            .setPositiveButton(R.string.ok) {_, _ -> }
            .show()
    }

    private fun onListItemClick(item: Item) {
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

    private fun onItemLongClick(item: Item): Boolean {
        AlertDialog.Builder(this).setTitle(R.string.uninstall_dialog_title)
            .setMessage(getString(R.string.uninstall_dialog_message, item.name))
            .setPositiveButton(R.string.ok) {_, _ ->
                adapter.gameList.uninstall(item)
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton(R.string.cancel) {_, _ -> }
            .show()
        return true
    }

    private fun showProgressDialog(savedInstanceState: Bundle? = null) {
        progressDialog = Dialog(this)
        progressDialog!!.apply {
            setTitle(R.string.install_dialog_title)
            setCancelable(false)
            setContentView(R.layout.progress_dialog)
            savedInstanceState?.let {
                findViewById<TextView>(R.id.text)?.text = it.getCharSequence(STATE_PROGRESS_TEXT)
            }
            show()
        }
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
}
