package io.github.kichikuou.xsystem4

import org.libsdl.app.SDLActivity

class XSystem4Activity : SDLActivity() {
    override fun getLibraries(): Array<String> {
        return arrayOf("SDL2", "xsystem4")
    }
}