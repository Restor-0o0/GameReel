package com.restor.gamereel.data.storage

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.restor.gamereel.data.storage.SqlWorkspaceRepository
import com.restor.gamereel.database.GameReelDatabase

fun createRepository(context: Context) =
    SqlWorkspaceRepository(
        AndroidSqliteDriver(GameReelDatabase.Schema, context.applicationContext, "gamereel.db")
    )
