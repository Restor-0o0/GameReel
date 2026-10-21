package com.restor.gamereel.data.storage

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.restor.gamereel.data.storage.SqlWorkspaceRepository
import com.restor.gamereel.database.GameReelDatabase
import java.io.File
import java.util.Properties

fun createRepository(): SqlWorkspaceRepository {
    val directory = File(System.getProperty("user.home"), ".gamereel")
    check(directory.isDirectory || directory.mkdirs()) { "cannot create data dir: $directory" }
    return SqlWorkspaceRepository(
        JdbcSqliteDriver(
            "jdbc:sqlite:${File(directory, "gamereel.db").absolutePath}",
            Properties(),
            GameReelDatabase.Schema,
        )
    )
}
