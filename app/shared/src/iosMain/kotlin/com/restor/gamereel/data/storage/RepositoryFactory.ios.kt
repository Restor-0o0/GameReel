package com.restor.gamereel.data.storage

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.restor.gamereel.data.storage.SqlWorkspaceRepository
import com.restor.gamereel.database.GameReelDatabase

fun createRepository() =
    SqlWorkspaceRepository(NativeSqliteDriver(GameReelDatabase.Schema, "gamereel.db"))
