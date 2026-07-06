package io.github.kichikuou.xsystem4

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
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

private class GameListAdapter(activity: Activity, private val gameList: GameList) : BaseAdapter() {
    companion object {
        private var defaultTextColor: ColorStateList? = null
    }
    private val context: Context = activity

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
        if (defaultTextColor == null)
            defaultTextColor = title.textColors
        title.text = item.name
        if (item.error != null)
            title.setTextColor(Color.GRAY)
        else
            title.setTextColor(defaultTextColor)
        return view
    }
}

class LauncherActivity : Activity(), GameListObserver {
    companion object {
        private var savedGameList: GameList? = null
    }

    private lateinit var gameList: GameList
    private lateinit var adapter: GameListAdapter
    private var progressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.launcher)

        gameList = getGameList(refresh = false)
        gameList.observer = this
        adapter = GameListAdapter(this, gameList)

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
        renderInstallState(gameList.installState)
    }

    override fun onDestroy() {
        if (gameList.observer === this) {
            gameList.observer = null
        }
        dismissProgressDialog()
        super.onDestroy()
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
                setGameList(getGameList(refresh = true))
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
                gameList.install(input)
                renderInstallState(gameList.installState)
            }
        }
    }

    override fun onInstallProgress(path: String) {
        renderInstallState(gameList.installState)
    }

    override fun onInstallSuccess() {
        renderInstallState(gameList.installState)
    }

    override fun onInstallFailure(msgId: Int) {
        renderInstallState(gameList.installState)
    }

    private fun getGameList(refresh: Boolean): GameList {
        if (savedGameList == null || refresh && savedGameList?.installState is InstallState.Idle) {
            savedGameList = GameList(this)
        }
        return savedGameList!!
    }

    private fun setGameList(newGameList: GameList) {
        if (gameList.observer === this) {
            gameList.observer = null
        }
        gameList = newGameList
        gameList.observer = this
        adapter = GameListAdapter(this, gameList)
        findViewById<ListView>(R.id.list).adapter = adapter
        renderInstallState(gameList.installState)
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
        i.putExtra(XSystem4Activity.EXTRA_SAVE_DIR, item.savedir!!.path)
        startActivity(i)
    }

    private fun onItemLongClick(item: Item): Boolean {
        AlertDialog.Builder(this).setTitle(R.string.uninstall_dialog_title)
            .setMessage(getString(R.string.uninstall_dialog_message, item.name))
            .setPositiveButton(R.string.ok) {_, _ ->
                gameList.uninstall(item)
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton(R.string.cancel) {_, _ -> }
            .show()
        return true
    }

    private fun renderInstallState(state: InstallState) {
        when (state) {
            InstallState.Idle -> dismissProgressDialog()
            is InstallState.Installing -> showProgressDialog(state.progress)
            InstallState.Succeeded -> {
                dismissProgressDialog()
                adapter.notifyDataSetChanged()
                gameList.consumeInstallResult()
            }
            is InstallState.Failed -> {
                dismissProgressDialog()
                AlertDialog.Builder(this).setTitle(R.string.error)
                    .setMessage(state.msgId)
                    .setPositiveButton(R.string.ok) {_, _ -> }
                    .show()
                gameList.consumeInstallResult()
            }
        }
    }

    private fun showProgressDialog(progress: String? = null) {
        if (progressDialog == null) {
            progressDialog = Dialog(this)
            progressDialog!!.apply {
                setTitle(R.string.install_dialog_title)
                setCancelable(false)
                setContentView(R.layout.progress_dialog)
                show()
            }
        }
        progress?.let {
            progressDialog?.findViewById<TextView>(R.id.text)?.text = getString(R.string.install_progress, it)
        }
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
}
