package org.publicvalue.multiplatform.qrcode

import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

@Composable
fun CameraView(
    modifier: Modifier = Modifier,
    analyzer: BarcodeAnalyzer,
    cameraPosition: CameraPosition,
    enableTorch: Boolean,
    cameraZoomState: CameraZoomState?,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember(context) {
        ProcessCameraProvider.getInstance(context)
    }

    val preview = remember { Preview.Builder().build() }
    val selector = remember(cameraPosition) {
        CameraSelector.Builder()
            .requireLensFacing(cameraPosition.toSelector())
            .build()
    }

    val imageAnalysis = remember(context, analyzer) {
        ImageAnalysis.Builder()
            .build()
            .also { it.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer) }
    }

    val camera = remember(cameraProviderFuture, imageAnalysis, selector) {
        runCatching {
            cameraProviderFuture.get().unbindAll()
            cameraProviderFuture.get().bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                imageAnalysis
            )
        }.onFailure {
            Log.e("CAMERA", "Camera bind error ${it.localizedMessage}", it)
        }
    }

    LaunchedEffect(camera, enableTorch) {
        camera.getOrNull()?.cameraControl?.enableTorch(enableTorch)
    }

    ObserveCameraZoom(camera, lifecycleOwner, cameraZoomState)

    LaunchedEffect(camera.getOrNull(), cameraZoomState) {
        val cam = camera.getOrNull() ?: return@LaunchedEffect
        val zoomState = cameraZoomState ?: return@LaunchedEffect
        snapshotFlow { zoomState.zoomRatio }.collectLatest { ratio ->
            withContext(Dispatchers.Main.immediate) {
                cam.cameraControl.setZoomRatio(ratio)
            }
        }
    }

    DisposableEffect(cameraProviderFuture) {
        onDispose {
            cameraProviderFuture.get().unbindAll()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            val previewView = PreviewView(context)
            preview.surfaceProvider = previewView.surfaceProvider
            previewView
        },
    )
}

@Composable
private fun ObserveCameraZoom(
    camera: Result<Camera>,
    lifecycleOwner: LifecycleOwner,
    cameraZoomState: CameraZoomState?,
) {
    DisposableEffect(camera.getOrNull(), lifecycleOwner, cameraZoomState) {
        val cam = camera.getOrNull()
        if (cam != null && cameraZoomState != null) {
            val observer = Observer<ZoomState> { state ->
                cameraZoomState.updateZoomRangeFromPlatform(
                    CameraZoomRange(
                        minZoomRatio = state.minZoomRatio,
                        maxZoomRatio = state.maxZoomRatio,
                    )
                )
            }
            cam.cameraInfo.zoomState.observe(lifecycleOwner, observer)
            onDispose {
                cam.cameraInfo.zoomState.removeObserver(observer)
                cameraZoomState.updateZoomRangeFromPlatform(null)
            }
        } else {
            if (cam == null && cameraZoomState != null) {
                cameraZoomState.updateZoomRangeFromPlatform(null)
            }
            onDispose { }
        }
    }
}

fun CameraPosition.toSelector() = when (this) {
    CameraPosition.FRONT -> CameraSelector.LENS_FACING_FRONT
    CameraPosition.BACK -> CameraSelector.LENS_FACING_BACK
}
