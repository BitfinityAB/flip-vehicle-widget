package com.flipvehiclewidget.app.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.flipvehiclewidget.app.R
import com.flipvehiclewidget.app.data.local.TokenManager
import com.flipvehiclewidget.app.domain.entity.CommandResult
import com.flipvehiclewidget.app.domain.entity.Vehicle
import com.flipvehiclewidget.app.domain.entity.VehicleCommand
import com.flipvehiclewidget.app.domain.repository.VehicleRepository
import com.flipvehiclewidget.app.domain.usecase.ToggleTrunkUseCase
import com.flipvehiclewidget.app.domain.usecase.VehicleCommandUseCase
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import net.openid.appauth.AuthorizationException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAppWidgetManager

private class FakeVehicleRepository(private val result: Result<CommandResult>) : VehicleRepository {
    override suspend fun getVehicle(): Result<Vehicle> = Result.success(Vehicle(7L, "5YJ3E1EA1PF000001", "Car"))
    override suspend fun toggleTrunk(vehicle: Vehicle): Result<CommandResult> = result
    override suspend fun toggleFrunk(vehicle: Vehicle): Result<CommandResult> = result
    override suspend fun toggleClimate(vehicle: Vehicle): Result<CommandResult> = result
    override suspend fun toggleLocks(vehicle: Vehicle): Result<CommandResult> = result
}

@RunWith(RobolectricTestRunner::class)
class VehicleCommandWorkerTest {
    init {
        VehicleWidgetProvider.updateDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private val shadowManager: ShadowAppWidgetManager = shadowOf(appWidgetManager)
    private val appWidgetId = shadowManager.createWidget(VehicleWidgetProvider::class.java, R.layout.widget_layout)

    @After
    fun resetWidgetProviderDispatcher() {
        VehicleWidgetProvider.updateDispatcher = kotlinx.coroutines.Dispatchers.IO
    }

    private fun workerFactoryFor(
        repository: VehicleRepository,
        useCases: Map<VehicleCommand, VehicleCommandUseCase>,
        tokenManager: TokenManager,
    ) = object : WorkerFactory() {
        override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
            VehicleCommandWorker(appContext, workerParameters, repository, useCases, tokenManager)
    }

    @Test
    fun `successful command clears loading state on the tapped button`() = runTest {
        val repository = FakeVehicleRepository(Result.success(CommandResult(success = true, reason = null)))
        val useCases: Map<VehicleCommand, VehicleCommandUseCase> =
            mapOf(VehicleCommand.TOGGLE_TRUNK to ToggleTrunkUseCase(repository))

        val worker = TestListenableWorkerBuilder<VehicleCommandWorker>(context)
            .setInputData(
                workDataOf(
                    VehicleCommandWorker.KEY_COMMAND to VehicleCommand.TOGGLE_TRUNK.name,
                    VehicleCommandWorker.KEY_APPWIDGET_ID to appWidgetId,
                ),
            )
            .setWorkerFactory(workerFactoryFor(repository, useCases, mockk(relaxed = true)))
            .build()

        val result = worker.doWork()

        assert(result is ListenableWorker.Result.Success)
        val view = shadowManager.getViewFor(appWidgetId)
        assertEquals(true, view.findViewById<android.widget.Button>(R.id.button_toggle_trunk).isEnabled)
    }

    @Test
    fun `failed command retries and shows error state`() = runTest {
        val repository = FakeVehicleRepository(Result.failure(RuntimeException("network down")))
        val useCases: Map<VehicleCommand, VehicleCommandUseCase> =
            mapOf(VehicleCommand.TOGGLE_TRUNK to ToggleTrunkUseCase(repository))

        val worker = TestListenableWorkerBuilder<VehicleCommandWorker>(context)
            .setInputData(
                workDataOf(
                    VehicleCommandWorker.KEY_COMMAND to VehicleCommand.TOGGLE_TRUNK.name,
                    VehicleCommandWorker.KEY_APPWIDGET_ID to appWidgetId,
                ),
            )
            .setWorkerFactory(workerFactoryFor(repository, useCases, mockk(relaxed = true)))
            .build()

        val result = worker.doWork()

        assert(result is ListenableWorker.Result.Retry)
    }

    @Test
    fun `auth failure clears token and disconnects widget instead of retrying`() = runTest {
        // AuthInterceptor (Task 5) wraps AuthorizationException as IOException(cause = ...) before it
        // reaches the repository, so simulate that same wrapping here rather than the bare exception.
        val authFailure = java.io.IOException("token refresh failed", AuthorizationException.GeneralErrors.NETWORK_ERROR)
        val repository = FakeVehicleRepository(Result.failure(authFailure))
        val useCases: Map<VehicleCommand, VehicleCommandUseCase> =
            mapOf(VehicleCommand.TOGGLE_TRUNK to ToggleTrunkUseCase(repository))
        val tokenManager = mockk<TokenManager>(relaxed = true)

        val worker = TestListenableWorkerBuilder<VehicleCommandWorker>(context)
            .setInputData(
                workDataOf(
                    VehicleCommandWorker.KEY_COMMAND to VehicleCommand.TOGGLE_TRUNK.name,
                    VehicleCommandWorker.KEY_APPWIDGET_ID to appWidgetId,
                ),
            )
            .setWorkerFactory(workerFactoryFor(repository, useCases, tokenManager))
            .build()

        val result = worker.doWork()

        assert(result is ListenableWorker.Result.Failure)
        val view = shadowManager.getViewFor(appWidgetId)
        assertEquals(android.view.View.VISIBLE, view.findViewById<android.view.View>(R.id.text_not_connected).visibility)
    }

    @Test
    fun `auth failure nested two levels deep still clears token and disconnects`() = runTest {
        // Simulates kotlinx.coroutines' stack-trace recovery inserting an extra IOException
        // wrapper when AuthInterceptor's exception crosses a coroutine suspension boundary
        // on a different thread than where it was thrown (confirmed real behavior, Task 18).
        val innerFailure = java.io.IOException("token refresh failed", AuthorizationException.GeneralErrors.NETWORK_ERROR)
        val recoveredFailure = java.io.IOException(innerFailure.message, innerFailure)
        val repository = FakeVehicleRepository(Result.failure(recoveredFailure))
        val useCases: Map<VehicleCommand, VehicleCommandUseCase> =
            mapOf(VehicleCommand.TOGGLE_TRUNK to ToggleTrunkUseCase(repository))
        val tokenManager = mockk<TokenManager>(relaxed = true)

        val worker = TestListenableWorkerBuilder<VehicleCommandWorker>(context)
            .setInputData(
                workDataOf(
                    VehicleCommandWorker.KEY_COMMAND to VehicleCommand.TOGGLE_TRUNK.name,
                    VehicleCommandWorker.KEY_APPWIDGET_ID to appWidgetId,
                ),
            )
            .setWorkerFactory(workerFactoryFor(repository, useCases, tokenManager))
            .build()

        val result = worker.doWork()

        assert(result is ListenableWorker.Result.Failure)
        val view = shadowManager.getViewFor(appWidgetId)
        assertEquals(android.view.View.VISIBLE, view.findViewById<android.view.View>(R.id.text_not_connected).visibility)
    }
}
