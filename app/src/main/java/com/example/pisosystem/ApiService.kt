package com.example.pisosystem

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class VoucherRequest(
    val code: String
)

data class VoucherResponse(
    val success: Boolean,
    val minutes: Int,
    val message: String
)

interface ApiService {

    @POST("connect")
    suspend fun connectVoucher(
        @Body request: VoucherRequest
    ): Response<VoucherResponse>
}