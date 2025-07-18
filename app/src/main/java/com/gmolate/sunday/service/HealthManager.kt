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

    suspend fun getHealthData(): Map<String, Any>? {
        val account = getGoogleAccount() ?: return null

        return try {
            val cal = Calendar.getInstance()
            val endTime = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, -30) // Últimos 30 días
            val startTime = cal.timeInMillis

            val readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_NUTRITION)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()

            val response = Fitness.getHistoryClient(context, account)
                .readData(readRequest)
                .await()

            // Procesar datos de nutrición
            val vitaminDData = mutableListOf<Float>()
            for (bucket in response.buckets) {
                for (dataSet in bucket.dataSets) {
                    for (dataPoint in dataSet.dataPoints) {
                        val nutrients = dataPoint.getValue(Field.FIELD_NUTRIENTS).asMap()
                        val vitaminD = nutrients["vitamin_d"] as? Float ?: 0f
                        if (vitaminD > 0) {
                            vitaminDData.add(vitaminD)
                        }
                    }
                }
            }

            mapOf(
                "vitamin_d_data" to vitaminDData,
                "total_vitamin_d" to vitaminDData.sum(),
                "average_vitamin_d" to if (vitaminDData.isNotEmpty()) vitaminDData.average() else 0.0,
                "days_with_data" to vitaminDData.size
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getTodayVitaminD(): Double {
        val account = getGoogleAccount() ?: return 0.0

        return try {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startTime = cal.timeInMillis

            cal.add(Calendar.DAY_OF_YEAR, 1)
            val endTime = cal.timeInMillis

            val readRequest = DataReadRequest.Builder()
                .read(DataType.TYPE_NUTRITION)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()

            val response = Fitness.getHistoryClient(context, account)
                .readData(readRequest)
                .await()

            var totalVitaminD = 0.0
            for (dataSet in response.dataSets) {
                for (dataPoint in dataSet.dataPoints) {
                    val nutrients = dataPoint.getValue(Field.FIELD_NUTRIENTS).asMap()
                    val vitaminD = nutrients["vitamin_d"] as? Float ?: 0f
                    totalVitaminD += vitaminD
                }
            }

            totalVitaminD
        } catch (e: Exception) {
            0.0
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

    fun hasHealthPermissions(): Boolean {
        val account = getGoogleAccount()
        return GoogleSignIn.hasPermissions(account, fitnessOptions)
    }

    fun requestHealthPermissions(activity: Activity, launcher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>) {
        val account = getGoogleAccount()
        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            val intent = GoogleSignIn.getClient(activity, GoogleSignIn.getAccountForExtension(activity, fitnessOptions))
                .signInIntent
            launcher.launch(intent)
        }
    }

    companion object {
        const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1001
    }
}
