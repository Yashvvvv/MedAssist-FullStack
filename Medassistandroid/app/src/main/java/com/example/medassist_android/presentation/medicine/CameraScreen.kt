package com.example.medassist_android.presentation.medicine

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.hilt.navigation.compose.hiltViewModel
import timber.log.Timber

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMedicineDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: MedicineViewModel = hiltViewModel()
    val aiAnalysisUiState by viewModel.aiAnalysisUiState.collectAsState()
    val permissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val previewView = remember { PreviewView(context) }
    val cameraProvider = remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(cameraProviderFuture) {
        cameraProvider.value = cameraProviderFuture.get()
        val preview = androidx.camera.core.Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        try {
            cameraProvider.value?.unbindAll()
            cameraProvider.value?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            Log.e("CameraScreen", "Use case binding failed", e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Medicine Scanner") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            if (permissionState.status.isGranted) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // Show loading indicator if analyzing
                if (aiAnalysisUiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                // Show error if any
                aiAnalysisUiState.error?.let { errorMsg ->
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                // Show result if available
                aiAnalysisUiState.analysisResult?.let { result ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Medicine Name: ${result.medicineName ?: "Unknown"}")
                            result.genericName?.let { Text("Generic: $it") }
                            result.manufacturer?.let { Text("Manufacturer: $it") }
                            result.description?.let { description ->
                                Text(
                                    text = "Description: ${description.take(100)}${if (description.length > 100) "..." else ""}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            result.confidenceScore?.let { confidence ->
                                Text("Confidence: ${String.format("%.0f", confidence * 100)}%")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onNavigateBack) {
                                Text("Continue")
                            }
                        }
                    }
                }

                IconButton(
                    onClick = {
                        takePhoto(
                            imageCapture = imageCapture,
                            context = context,
                            onImageCaptured = { uri ->
                                val file = File(uri.path ?: return@takePhoto)
                                viewModel.analyzeByImage(file)
                            },
                            onError = { error ->
                                Timber.e(error, "Error capturing image")
                            }
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .size(80.dp)
                        .border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "Take photo",
                        modifier = Modifier.size(48.dp)
                    )
                }

            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This feature requires camera access to scan medicines. Please grant the permission.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { permissionState.launchPermissionRequest() }) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}

private fun takePhoto(
    imageCapture: ImageCapture,
    context: Context,
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val photoFile = File(
        context.filesDir,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                onImageCaptured(savedUri)
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}
