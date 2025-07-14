package com.gmolate.sunday.service

import android.app.Activity
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
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

    private fun getGoogleAccount() = GoogleSignIn.getLastSignedInAccount(context)

    suspend fun saveVitaminD(amount: Double) {
        val account = getGoogleAccount() ?: return
        val nutritionSource = com.google.android.gms.fitness.data.DataSource.Builder()
            .setAppPackageName(context)
            .setDataType(DataType.TYPE_NUTRITION)
            .setType(com.google.android.gms.fitness.data.DataSource.TYPE_RAW)
            .build()

        val dataPoint = com.google.android.gms.fitness.data.DataPoint.builder(nutritionSource)
            .setTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            .setField(Field.FIELD_NUTRIENTS, mapOf(Field.NUTRIENT_VITAMIN_D to amount.toFloat()))
            .build()

        Fitness.getHistoryClient(context, account).insertData(dataPoint).await()
    }

    suspend fun getAgeFactor(): Double {
        // In a real app, you would get the user's age from their profile.
        // For now, we'll just return 1.0
        return 1.0
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

        val response = Fitness.getHistoryClient(context, account).readData(readRequest).await()
        val totalVitaminD = response.dataSets
            .flatMap { it.dataPoints }
            .sumOf { it.getValue(Field.FIELD_NUTRIENTS).getKeyValue(Field.NUTRIENT_VITAMIN_D)?.toDouble() ?: 0.0 }

        val averageDailyExposure = totalVitaminD / 7.0
        return when {
            averageDailyExposure < 1000 -> 0.8
            averageDailyExposure >= 10000 -> 1.2
            else -> 0.8 + (averageDailyExposure - 1000) / 9000 * 0.4
        }
    }

    fun hasPermissions(): Boolean {
        return GoogleSignIn.hasPermissions(getGoogleAccount(), fitnessOptions)
    }

    fun requestPermissions(activity: Activity) {
        GoogleSignIn.requestPermissions(
            activity,
            1,
            getGoogleAccount(),
            fitnessOptions
        )
    }
}
