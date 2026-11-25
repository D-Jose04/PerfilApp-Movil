package com.universidad.perfilapp.data.repository

import com.google.gson.Gson
import com.universidad.perfilapp.data.remote.ApiService
import com.universidad.perfilapp.data.model.ErrorResponse
import com.universidad.perfilapp.data.model.LoginRequest
import com.universidad.perfilapp.data.model.LoginResponse
import com.universidad.perfilapp.data.model.RegisterRequest
import com.universidad.perfilapp.data.model.RegisterResponse
import com.universidad.perfilapp.data.remote.RetrofitClient
import com.universidad.perfilapp.data.model.UpdatePhotoRequest
import com.universidad.perfilapp.data.model.UpdatePhotoResponse
import com.universidad.perfilapp.data.model.UserDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

class UserRepository(
    private val apiService: ApiService = RetrofitClient.apiService
) {

    private val gson = Gson()

    suspend fun register(
        fullName: String,
        email: String,
        password: String
    ): Result<RegisterResponse> = safeApiCall {
        apiService.register(RegisterRequest(fullName, email, password))
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<LoginResponse> = safeApiCall {
        apiService.login(LoginRequest(email, password))
    }

    suspend fun getProfile(token: String): Result<UserDto> = safeApiCall {
        apiService.getProfile("Bearer $token")
    }

    suspend fun updateProfilePhoto(
        token: String,
        imageBase64: String
    ): Result<UpdatePhotoResponse> = safeApiCall {
        apiService.updateProfilePhoto(
            bearerToken = "Bearer $token",
            request = UpdatePhotoRequest(imageBase64)
        )
    }

    private suspend fun <T> safeApiCall(
        call: suspend () -> Response<T>
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("La respuesta del servidor llegó vacía."))
                }
            } else {
                val errorText = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorText))
            }
        } catch (e: IOException) {
            Result.failure(
                Exception("No se pudo conectar con el servidor. Verifica que el backend esté activo.")
            )
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Error inesperado en la comunicación."))
        }
    }

    private fun parseErrorMessage(errorBody: String?, statusCode: Int): String {
        if (!errorBody.isNullOrBlank()) {
            val parsed = runCatching {
                gson.fromJson(errorBody, ErrorResponse::class.java)
            }.getOrNull()
            if (!parsed?.message.isNullOrBlank()) {
                return parsed.message
            }
        }
        return "Error del servidor (código HTTP $statusCode)."
    }
}