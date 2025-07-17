package com.gmolate.sunday.service

import android.app.Activity
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.request.DataReadRequest
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.concurrent.TimeUnit

class HealthManager(private val context: Context) {

    private val fitnessOptions: FitnessOptions by lazy {
        FitnessOptions.builder()
            .addDataType(DataType.TYPE_NUTRITION, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_NUTRITION, FitnessOptions.ACCESS_WRITE)
            .build()
    }

    private fun getGoogleAccount() = GoogleSignIn.getAccountForExtension(context, fitnessOptions)

    suspend fun saveVitaminD(amount: Double) {
        val account = getGoogleAccount() ?: return
        val nutritionSource = DataSource.Builder()
            .setAppPackageName(context.packageName)
            .setDataType(DataType.TYPE_NUTRITION)
            .setType(DataSource.TYPE_RAW)
            .build()

        val nutrients = HashMap<String, Float>()
        nutrients["vitamin_d"] = amount.toFloat()

        val dataPoint = DataPoint.builder(nutritionSource)
            .setTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            .setField(Field.FIELD_NUTRIENTS, nutrients)
            .build()

        val dataSet = DataSet.builder(nutritionSource)
            .add(dataPoint)
            .build()

        try {
            Fitness.getHistoryClient(context, account)
                .insertData(dataSet)
                .await()
        } catch (e: Exception) {
            // Manejo de error
        }
    }

    suspend fun saveVitaminDSession(amount: Double) {
        if (amount <= 0) return
        saveVitaminD(amount)
    }

    suspend fun getTodayVitaminD(): Double {
        val account = getGoogleAccount() ?: return 0.0

        val endTime = System.currentTimeMillis()
        val startTime = Calendar.getInstance().apply {
            timeInMillis = endTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        val readRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_NUTRITION)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        try {
            val response = Fitness.getHistoryClient(context, account)
                .readData(readRequest)
                .await()

            var totalVitaminD = 0.0
            response.dataSets.forEach { dataSet ->
                dataSet.dataPoints.forEach { point ->
                    point.getValue(Field.FIELD_NUTRIENTS)?.let { nutrients ->
                        @Suppress("UNCHECKED_CAST")
                        val map = nutrients as Map<String, Float>
                        totalVitaminD += (map["vitamin_d"] ?: 0f).toDouble()
                    }
                }
            }
            return totalVitaminD
        } catch (e: Exception) {
            return 0.0
        }
    }

    suspend fun getAdaptationFactor(): Double {
        val account = getGoogleAccount() ?: return 1.0
        val cal = Calendar.getInstance()
        val endTime = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = cal.timeInMillis

        val readRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_NUTRITION)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        try {
            val response = Fitness.getHistoryClient(context, account)
                .readData(readRequest)
                .await()

            val totalVitaminD = response.dataSets
                .flatMap { it.dataPoints }
                .sumOf { dataPoint ->
                    val nutrients = dataPoint.getValue(Field.FIELD_NUTRIENTS) as? Map<String, Float>
                    nutrients?.get("vitamin_d")?.toDouble() ?: 0.0
                }

            val averageDailyExposure = totalVitaminD / 7.0
            return when {
                averageDailyExposure < 1000 -> 0.8
                averageDailyExposure >= 10000 -> 1.2
                else -> 0.8 + (averageDailyExposure - 1000) / 9000 * 0.4
            }
        } catch (e: Exception) {
            return 1.0
        }
    }

    suspend fun getAgeFactor(): Double {
        // Por ahora retornamos un valor fijo, pero después se podría calcular según la edad del usuario
        return 1.0
    }

    fun hasPermissions(): Boolean {
        val account = getGoogleAccount()
        return account != null && GoogleSignIn.hasPermissions(account, fitnessOptions)
    }

    fun requestPermissions(activity: Activity) {
        val account = getGoogleAccount()
        if (account != null && !GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                activity,
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                account,
                fitnessOptions
            )
        }
    }

    companion object {
        const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1001
    }
}
