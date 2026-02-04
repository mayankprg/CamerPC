package com.fumes.camerpc.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fumes.camerpc.util.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fumes.camerpc.data.CameraOption
import com.fumes.camerpc.data.VideoEncoder


@Composable
fun MainScreen() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(factory = MainViewModel.provideFactory(context))
    val uiState by viewModel.uiState.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            viewModel.onPermissionResult(granted)
            if (!granted) {
                Toast.makeText(context, "Camera permission is required", Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.onPermissionResult(true)
        } else {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (uiState.hasCameraPermission) {
        CameraContent(
            isStreaming = uiState.isStreaming,
            ipAddress = uiState.ipAddress,
            availableCameras = uiState.availableCameras,
            selectedCamera = uiState.selectedCamera,
            forceKeyFrameEvent = viewModel.forceKeyFrameEvent,
            onCameraSelected = viewModel::selectCamera,
            onEncodedData = { data, isKeyFrame -> viewModel.sendEncodedData(data, isKeyFrame) },
            onToggleStreaming = viewModel::toggleStreaming
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera Permission Needed", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
fun CameraContent(
    isStreaming: Boolean,
    ipAddress: String,
    availableCameras: List<CameraOption>,
    selectedCamera: CameraOption?,
    forceKeyFrameEvent: kotlinx.coroutines.flow.SharedFlow<Unit>,
    onCameraSelected: (CameraOption) -> Unit,
    onEncodedData: (ByteArray, Boolean) -> Unit,
    onToggleStreaming: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val videoEncoder = remember { VideoEncoder() }
    // executor not needed for Surface mode
    
    LaunchedEffect(Unit) {
        forceKeyFrameEvent.collect {
            videoEncoder.forceKeyFrame()
        }
    }
    
    LaunchedEffect(selectedCamera, isStreaming) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).await()
        val preview = Preview.Builder().build()
        val selector = selectedCamera?.selector ?: CameraSelector.DEFAULT_BACK_CAMERA

        preview.setSurfaceProvider(previewView.surfaceProvider)

        val useCases = mutableListOf<androidx.camera.core.UseCase>(preview)

        if (isStreaming) {
            val encoderPreview = Preview.Builder().build()
            
            encoderPreview.setSurfaceProvider { request ->
                val resolution = request.resolution
                val encoderSurface = videoEncoder.start(resolution.width, resolution.height) { data, isKeyFrame ->
                     // Send encoded data
                     onEncodedData(data, isKeyFrame)
                }
                
                if (encoderSurface != null) {
                    request.provideSurface(encoderSurface, ContextCompat.getMainExecutor(context)) { result ->
                        // Surface release or cleanup if needed
                        // VideoEncoder.stop() handles surface release internally when stopped, 
                        // but strictly speaking, we passed the surface to CameraX.
                        // When CameraX is done, it calls this. We don't need to do much 
                        // as VideoEncoder manages the Codec and Surface lifecycle.
                    }
                } else {
                    request.willNotProvideSurface()
                }
            }
            useCases.add(encoderPreview)
        } else {
            videoEncoder.stop()
        }

        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                selector,
                *useCases.toTypedArray()
            )
            
            if (selectedCamera?.zoomRatio != null) {
                camera.cameraControl.setZoomRatio(selectedCamera.zoomRatio)
            }
        } catch (e: Exception) {
            Log.e("CameraContent", "Use case binding failed", e)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { 
                previewView.apply { 
                    keepScreenOn = true 
                } 
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isStreaming) "Streaming Active" else "Ready to Stream",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isStreaming) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Connect to: $ipAddress:5000",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (availableCameras.isNotEmpty()) {
                        CameraSelectorDropdown(
                            availableCameras = availableCameras,
                            selectedCamera = selectedCamera,
                            onCameraSelected = onCameraSelected
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Button(
                        onClick = onToggleStreaming,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isStreaming) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isStreaming) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(if (isStreaming) "Stop Streaming" else "Start Streaming")
                    }
                }
            }
        }
    }
}

@Composable
fun CameraSelectorDropdown(
    availableCameras: List<CameraOption>,
    selectedCamera: CameraOption?,
    onCameraSelected: (CameraOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopStart)
    ) {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Text(
                text = selectedCamera?.title ?: "Select Camera",
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "Select Camera",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            availableCameras.forEach { camera ->
                DropdownMenuItem(
                    text = { Text(camera.title) },
                    onClick = {
                        onCameraSelected(camera)
                        expanded = false
                    }
                )
            }
        }
    }
}