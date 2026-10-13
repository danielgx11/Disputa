package disputa.storage

import java.io.File

class JvmPhotoStore(private val root: File) : PhotoStore {
    init {
        root.mkdirs()
    }

    override fun save(teamId: String, bytes: ByteArray): String {
        val file = File(root, "$teamId.jpg")
        file.writeBytes(bytes)
        return file.name
    }

    override fun delete(relativePath: String) {
        File(root, relativePath.substringAfterLast('/')).delete()
    }

    override fun resolve(relativePath: String): String =
        File(root, relativePath.substringAfterLast('/')).absolutePath
}
