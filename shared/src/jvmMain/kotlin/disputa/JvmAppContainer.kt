package disputa

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import disputa.storage.DisputaDatabase
import disputa.storage.JvmPhotoStore
import java.io.File
import kotlin.io.path.createTempDirectory

fun createJvmAppContainer(
    photoDir: File = createTempDirectory("disputa-photos").toFile(),
): AppContainer {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    DisputaDatabase.Schema.create(driver)
    return AppContainer(driver, JvmPhotoStore(photoDir))
}
