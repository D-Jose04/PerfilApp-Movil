package com.universidad.perfilapp.data.remote

import com.universidad.perfilapp.data.model.LoginRequest
import com.universidad.perfilapp.data.model.LoginResponse
import com.universidad.perfilapp.data.model.RegisterRequest
import com.universidad.perfilapp.data.model.RegisterResponse
import com.universidad.perfilapp.data.model.UpdatePhotoRequest
import com.universidad.perfilapp.data.model.UpdatePhotoResponse
import com.universidad.perfilapp.data.model.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

interface ApiService {

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("api/profile")
    suspend fun getProfile(
        @Header("Authorization") bearerToken: String
    ): Response<UserDto>

    @PUT("api/profile/photo")
    suspend fun updateProfilePhoto(
        @Header("Authorization") bearerToken: String,
        @Body request: UpdatePhotoRequest
    ): Response<UpdatePhotoResponse>
}