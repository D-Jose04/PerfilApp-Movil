package com.universidad.perfilapp.data.model

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class UpdatePhotoRequest(
    val imageBase64: String
)

data class UserDto(
    val id: Int,
    val fullName: String,
    val email: String,
    val profileImageBase64: String? = null,
    val createdAt: String? = null
)

data class RegisterResponse(
    val message: String,
    val user: UserDto
)

data class LoginResponse(
    val message: String,
    val token: String,
    val user: UserDto
)

data class UpdatePhotoResponse(
    val message: String,
    val profileImageBase64: String
)

data class ErrorResponse(
    val message: String?
)