package com.example.sundayandroidapp.api

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApiService {
    @GET("v1/forecast?current=uv_index,cloud_cover&daily=uv_index_max,sunrise,sunset&timezone=auto")
    suspend fun getSunData(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): OpenMeteoResponse
}
