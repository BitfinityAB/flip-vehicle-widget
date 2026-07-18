package com.flipvehiclewidget.app.di

import com.flipvehiclewidget.app.data.bluetooth.AndroidBluetoothConnectivityGateway
import com.flipvehiclewidget.app.data.bluetooth.BluetoothConnectivityGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BluetoothModule {
    @Binds
    @Singleton
    abstract fun bindBluetoothConnectivityGateway(
        impl: AndroidBluetoothConnectivityGateway,
    ): BluetoothConnectivityGateway
}
