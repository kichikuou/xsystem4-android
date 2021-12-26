package io.github.kichikuou.xsystem4

import android.util.Log
import org.libsdl.app.SDLActivity

// Intent for this activity must have the following extras:
// - EXTRA_GAME_ROOT (string): A path to the game installation.
class XSystem4Activity : SDLActivity() {
    companion object {
        const val EXTRA_GAME_ROOT = "GAME_ROOT"
    }

    override fun getLibraries(): Array<String> {
        return arrayOf("SDL2", "xsystem4")
    }

    override fun getArguments(): Array<String> {
        val gameRoot = intent.getStringExtra(EXTRA_GAME_ROOT)!!
        return arrayOf(gameRoot)
    }
}