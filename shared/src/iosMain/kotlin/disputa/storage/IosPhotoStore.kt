package disputa.storage

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class)
class IosPhotoStore : PhotoStore {
    private val directory: String by lazy {
        val url = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        val path = (url?.path ?: ".") + "/photos"
        NSFileManager.defaultManager.createDirectoryAtPath(path, true, null, null)
        path
    }

    override fun save(teamId: String, bytes: ByteArray): String {
        val relative = "photos/$teamId.jpg"
        val full = "$directory/$teamId.jpg"
        bytes.usePinned { pinned ->
            val data = NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            data.writeToFile(full, atomically = true)
        }
        return relative
    }

    override fun delete(relativePath: String) {
        val name = relativePath.substringAfterLast('/')
        NSFileManager.defaultManager.removeItemAtPath("$directory/$name", null)
    }

    override fun resolve(relativePath: String): String {
        val name = relativePath.substringAfterLast('/')
        return "$directory/$name"
    }
}
