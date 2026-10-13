package disputa

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import disputa.storage.DisputaDatabase
import disputa.storage.IosPhotoStore

fun createIosAppContainer(): AppContainer {
    val driver = NativeSqliteDriver(DisputaDatabase.Schema, "disputa.db")
    return AppContainer(driver, IosPhotoStore())
}
