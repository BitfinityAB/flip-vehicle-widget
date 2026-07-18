package com.flipvehiclewidget.app.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flipvehiclewidget.app.data.local.TokenManager
import com.flipvehiclewidget.app.domain.entity.VehicleCommand
import com.flipvehiclewidget.app.domain.repository.VehicleRepository
import com.flipvehiclewidget.app.domain.usecase.VehicleCommandUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import net.openid.appauth.AuthorizationException
import timber.log.Timber

@HiltWorker
class VehicleCommandWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: VehicleRepository,
    private val useCases: Map<VehicleCommand, @JvmSuppressWildcards VehicleCommandUseCase>,
    private val tokenManager: TokenManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val commandName = inputData.getString(KEY_COMMAND) ?: return Result.failure()
        val appWidgetId = inputData.getInt(KEY_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val command = runCatching { VehicleCommand.valueOf(commandName) }.getOrNull() ?: return Result.failure()

        val vehicleResult = repository.getVehicle()
        val vehicle = vehicleResult.getOrElse { error ->
            Timber.w(error, "Failed to resolve vehicle for command %s", command)
            return handleFailure(appWidgetId, command, error)
        }

        val useCase = useCases[command] ?: return Result.failure()
        val outcome = useCase(vehicle.id)

        return outcome.fold(
            onSuccess = { commandResult ->
                val buttonState = if (commandResult.success) CommandButtonState.IDLE else CommandButtonState.ERROR
                render(appWidgetId, command, buttonState)
                Result.success()
            },
            onFailure = { error ->
                Timber.w(error, "Command %s failed", command)
                handleFailure(appWidgetId, command, error)
            },
        )
    }

    private fun handleFailure(appWidgetId: Int, command: VehicleCommand, error: Throwable): Result {
        // AuthInterceptor (Task 5) wraps token-fetch failures as IOException(cause = AuthorizationException)
        // rather than letting them propagate as their original type, so check the cause here.
        if (error.cause is AuthorizationException) {
            tokenManager.clear()
            renderDisconnected(appWidgetId)
            return Result.failure()
        }
        render(appWidgetId, command, CommandButtonState.ERROR)
        return Result.retry()
    }

    private fun render(appWidgetId: Int, command: VehicleCommand, buttonState: CommandButtonState) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        VehicleWidgetProvider.render(
            applicationContext,
            AppWidgetManager.getInstance(applicationContext),
            appWidgetId,
            WidgetState.Connected(commandStates = mapOf(command to buttonState)),
        )
    }

    private fun renderDisconnected(appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        VehicleWidgetProvider.render(
            applicationContext,
            AppWidgetManager.getInstance(applicationContext),
            appWidgetId,
            WidgetState.Disconnected,
        )
    }

    companion object {
        const val KEY_COMMAND = "command"
        const val KEY_APPWIDGET_ID = "appWidgetId"
        const val WORK_TAG = "vehicle_command_worker"
    }
}
