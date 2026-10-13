plugins {
    kotlin("multiplatform")
    id("app.cash.sqldelight")
}

kotlin {
    jvm()

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
            implementation("app.cash.sqldelight:runtime:2.2.1")
            implementation("app.cash.sqldelight:coroutines-extensions:2.2.1")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
        jvmMain.dependencies {
            implementation("app.cash.sqldelight:sqlite-driver:2.2.1")
        }
        jvmTest.dependencies {
            implementation("app.cash.sqldelight:sqlite-driver:2.2.1")
        }
        iosMain.dependencies {
            implementation("app.cash.sqldelight:native-driver:2.2.1")
        }
    }
}

sqldelight {
    databases {
        create("DisputaDatabase") {
            packageName.set("disputa.storage")
        }
    }
}
