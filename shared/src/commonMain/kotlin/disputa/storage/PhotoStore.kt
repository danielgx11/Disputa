package disputa.storage

interface PhotoStore {
    fun save(teamId: String, bytes: ByteArray): String
    fun delete(relativePath: String)
    fun resolve(relativePath: String): String
}
