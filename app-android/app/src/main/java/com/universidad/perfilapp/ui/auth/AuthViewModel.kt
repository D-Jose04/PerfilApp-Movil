package com.universidad.perfilapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universidad.perfilapp.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class RegisterSuccess(val message: String) : AuthUiState
    data class LoginSuccess(val token: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun register(fullName: String, email: String, password: String) {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim()

        if (cleanName.isBlank() || cleanEmail.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Todos los campos son obligatorios.")
            return
        }
        if (!emailRegex.matches(cleanEmail)) {
            _uiState.value = AuthUiState.Error("Ingresa un correo electrónico válido.")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("La contraseña debe tener al menos 6 caracteres.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.register(cleanName, cleanEmail, password)
            _uiState.value = result.fold(
                onSuccess = { AuthUiState.RegisterSuccess(it.message) },
                onFailure = { AuthUiState.Error(it.message ?: "No se pudo completar el registro.") }
            )
        }
    }

    fun login(email: String, password: String) {
        val cleanEmail = email.trim()

        if (cleanEmail.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Ingresa tu correo y contraseña.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.login(cleanEmail, password)
            _uiState.value = result.fold(
                onSuccess = { AuthUiState.LoginSuccess(it.token) },
                onFailure = { AuthUiState.Error(it.message ?: "No se pudo iniciar sesión.") }
            )
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}