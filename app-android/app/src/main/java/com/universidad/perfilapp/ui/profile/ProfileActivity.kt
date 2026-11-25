package com.universidad.perfilapp.ui.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.universidad.perfilapp.R
import com.universidad.perfilapp.data.local.SessionManager
import com.universidad.perfilapp.databinding.ActivityProfileBinding
import com.universidad.perfilapp.ui.auth.LoginActivity
import com.universidad.perfilapp.util.ImageBase64Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: ProfileViewModel by viewModels()

    private var lastRenderedBase64: String? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            takePictureLauncher.launch(null)
        } else {
            Toast.makeText(
                this,
                getString(R.string.msg_camera_permission_denied),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Captura en memoria sin almacenar archivos en el dispositivo
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap == null) return@registerForActivityResult
        val token = sessionManager.getToken() ?: return@registerForActivityResult

        lifecycleScope.launch {
            val base64 = withContext(Dispatchers.Default) {
                ImageBase64Utils.bitmapToBase64(bitmap)
            }
            viewModel.updatePhoto(token, base64)
        }
    }

    // Selector oficial Photo Picker (sin permisos de almacenamiento)
    private val pickMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val token = sessionManager.getToken() ?: return@registerForActivityResult

        lifecycleScope.launch {
            val base64 = withContext(Dispatchers.IO) {
                ImageBase64Utils.uriToBase64(contentResolver, uri)
            }
            if (base64 != null) {
                viewModel.updatePhoto(token, base64)
            } else {
                Toast.makeText(
                    this@ProfileActivity,
                    getString(R.string.msg_image_process_error),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        val token = sessionManager.getToken()
        if (token.isNullOrBlank()) {
            redirectToLogin()
            return
        }

        enableEdgeToEdge()
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemInsets(binding.main)

        setupListeners()
        observeUiState()

        if (viewModel.uiState.value.user == null) {
            viewModel.loadProfile(token)
        }
    }

    private fun setupListeners() {
        binding.btnTakePhoto.setOnClickListener {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                takePictureLauncher.launch(null)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnPickGallery.setOnClickListener {
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            redirectToLogin()
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.btnTakePhoto.isEnabled = !state.isLoading
                    binding.btnPickGallery.isEnabled = !state.isLoading

                    state.user?.let { user ->
                        binding.tvFullName.text = user.fullName
                        binding.tvEmail.text = user.email
                        binding.tvMemberSince.text = if (!user.createdAt.isNullOrBlank()) {
                            getString(R.string.label_member_since, user.createdAt)
                        } else {
                            ""
                        }
                        renderProfileImage(user.profileImageBase64)
                    }

                    state.statusMessage?.let { message ->
                        Toast.makeText(this@ProfileActivity, message, Toast.LENGTH_SHORT).show()
                        viewModel.clearMessages()
                    }

                    state.errorMessage?.let { error ->
                        Toast.makeText(this@ProfileActivity, error, Toast.LENGTH_LONG).show()
                        viewModel.clearMessages()
                    }
                }
            }
        }
    }

    private fun renderProfileImage(base64: String?) {
        if (base64 == lastRenderedBase64) return
        lastRenderedBase64 = base64

        if (base64.isNullOrBlank()) {
            binding.ivProfilePhoto.setImageResource(R.drawable.ic_person_placeholder)
            return
        }

        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                ImageBase64Utils.base64ToBitmap(base64)
            }
            if (bitmap != null) {
                binding.ivProfilePhoto.setImageBitmap(bitmap)
            } else {
                binding.ivProfilePhoto.setImageResource(R.drawable.ic_person_placeholder)
            }
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun applySystemInsets(view: View) {
        val initialLeft = view.paddingLeft
        val initialTop = view.paddingTop
        val initialRight = view.paddingRight
        val initialBottom = view.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                initialLeft + bars.left,
                initialTop + bars.top,
                initialRight + bars.right,
                initialBottom + bars.bottom
            )
            insets
        }
    }
}