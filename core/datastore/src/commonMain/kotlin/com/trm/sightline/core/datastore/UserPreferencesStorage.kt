package com.trm.sightline.core.datastore

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import com.trm.sightline.core.model.UserPreferences

const val USER_PREFERENCES_DIRECTORY = "datastore"
const val USER_PREFERENCES_FILE_NAME = "user_preferences.json"

expect fun userPreferencesStorage(directoryPath: String): Storage<UserPreferences>

fun createUserPreferencesDataStore(
  storage: Storage<UserPreferences>,
  migrations: List<DataMigration<UserPreferences>> = emptyList(),
): DataStore<UserPreferences> = DataStoreFactory.create(storage = storage, migrations = migrations)
