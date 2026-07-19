package com.flipvehiclewidget.app.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Plain, unencrypted -- unlike @EncryptedPrefs (see SecurityModule) -- for values that aren't
// secrets (vehicle VIN, BLE beacon proximity state), so this doesn't need a real Android
// Keystore the way EncryptedSharedPreferences does (which Robolectric can't provide).
@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    @Provides
    @Singleton
    @AppPrefs
    fun provideAppPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("flip_vehicle_widget_prefs", Context.MODE_PRIVATE)
}
