package io.github.kichikuou.xsystem4

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun ByteBuffer.skip(n: Int) {
    this.position(this.position() + n)
}

class PEResourceExtractor(val buf: ByteBuffer, private val rvaDelta: Int) {
    companion object {
        // Resource types.
        const val RT_ICON = 3
        const val RT_GROUP_ICON = 14

        // Returns null if the given file is not in PE format or has no resource section.
        fun create(exeFile: File): PEResourceExtractor? {
            val bytes = exeFile.readBytes()
            val buf = ByteBuffer.wrap(bytes)
            buf.order(ByteOrder.LITTLE_ENDIAN)
            // MS-DOS Stub
            if (buf.short != 0x5a4d.toShort()) {  // "MZ"
                return null
            }
            val ofsPe = buf.getInt(0x3c)
            // PE signature
            if (buf.getInt(ofsPe) != 0x4550) {  // "PE\0\0"
                return null
            }
            // COFF file header
            val ofsCoff = ofsPe + 4
            val numSections = buf.getShort(ofsCoff + 2)
            val sizeOfOptionalHeader = buf.getShort(ofsCoff + 16)
            buf.position(ofsCoff + 20 + sizeOfOptionalHeader)
            // Section table
            val sectionName = ByteArray(8)
            repeat(numSections.toInt()) {
                buf.get(sectionName)
                if (String(sectionName) == ".rsrc\u0000\u0000\u0000") {
                    buf.skip(4)
                    val virtualAddress = buf.int
                    val sizeOfRawData = buf.int
                    val pointerToRawData = buf.int
                    buf.position(pointerToRawData)
                    buf.limit(pointerToRawData + sizeOfRawData)
                    buf.mark()
                    return PEResourceExtractor(buf, virtualAddress - pointerToRawData)
                }
                buf.skip(32)
            }
            return null
        }
    }

    data class ResourceDirectoryEntry(val nameOrId: Int, val isDirectory: Boolean, val offset: Int)
    inner class ResourceDirectory(offsetFromSectionTop: Int) {
        private val entries = arrayListOf<ResourceDirectoryEntry>()
        init {
            val buf = this@PEResourceExtractor.buf
            buf.reset()
            buf.skip(offsetFromSectionTop + 12)
            val numNamedEntries = buf.short
            val numIdEntries = buf.short
            repeat(numNamedEntries + numIdEntries) {
                val nameOrId = buf.int
                val ofs = buf.int
                if (ofs < 0) {
                    entries.add(ResourceDirectoryEntry(nameOrId, true, ofs and 0x7fffffff))
                } else {
                    entries.add(ResourceDirectoryEntry(nameOrId, false, ofs))
                }
            }
        }

        fun getDirectory(nameOrId: Int): ResourceDirectory? {
            return entries.find {
                it.nameOrId == nameOrId && it.isDirectory
            }?.let { ResourceDirectory(it.offset) }
        }

        fun getResource(nameOrId: Int): ByteArray? {
            return entries.find {
                it.nameOrId == nameOrId && !it.isDirectory
            }?.let { this@PEResourceExtractor.getResource(it.offset) }
        }

        fun getFirstResource(): ByteArray {
            return if (entries[0].isDirectory) {
                ResourceDirectory(entries[0].offset).getFirstResource()
            } else {
                this@PEResourceExtractor.getResource(entries[0].offset)
            }
        }
    }

    private fun getRoot() = ResourceDirectory(0)
    fun getDirectory(type: Int): ResourceDirectory? = getRoot().getDirectory(type)

    fun getResource(offset: Int): ByteArray {
        buf.reset()
        buf.skip(offset)
        val offsetInFile = buf.int - rvaDelta
        val size = buf.int
        buf.position(offsetInFile)
        val data = ByteArray(size)
        buf.get(data)
        return data
    }

    // Reconstruct an icon file from RT_GROUP_ICON / RT_ICON resources.
    // https://devblogs.microsoft.com/oldnewthing/20120720-00/?p=7083
    fun extractIcon(): ByteArray? {
        val iconResources = getDirectory(RT_ICON) ?: return null
        val grpIconBytes = getDirectory(RT_GROUP_ICON)?.getFirstResource() ?: return null
        val buf = ByteBuffer.wrap(grpIconBytes)
        buf.order(ByteOrder.LITTLE_ENDIAN)

        val out = ByteArrayOutputStream()
        val numIcons = buf.getShort(4).toInt()
        out.write(grpIconBytes, 0, 6)
        buf.position(6)

        var offsetInFile = 6 + numIcons * 16
        val entryBytes = ByteArray(12)
        val icons = Array(numIcons) {
            buf.get(entryBytes)
            out.write(entryBytes)
            out.write(offsetInFile and 0xff)
            out.write((offsetInFile shr 8) and 0xff)
            out.write((offsetInFile shr 16) and 0xff)
            out.write((offsetInFile shr 24) and 0xff)
            val iconId = buf.short.toInt()
            val iconBytes = iconResources.getDirectory(iconId)?.getFirstResource() ?: return null
            offsetInFile += iconBytes.size
            iconBytes
        }
        for (icon in icons) {
            out.write(icon)
        }
        return out.toByteArray()
    }
}
