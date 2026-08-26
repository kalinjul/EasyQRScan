package org.publicvalue.multiplatform.qrcode

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraView(
    modifier: Modifier = Modifier,
    analyzer: BarcodeAnalyzer,
    cameraPosition: CameraPosition,
    enableTorch: Boolean,
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
            .also {  it.setAnalyzer( ContextCompat.getMainExecutor(context), analyzer ) }
    }

    // ViewPort ties the Preview and ImageAnalysis crop rects to the same field of view, so
    // fractions applied to ImageAnalysis frames (see BarcodeAnalyzer) match what is visible
    // on screen. It becomes available once the PreviewView has been laid out.
    var viewPort by remember { mutableStateOf<ViewPort?>(null) }

    val camera = remember(cameraProviderFuture, imageAnalysis, selector, viewPort) {
        runCatching {
            cameraProviderFuture.get().unbindAll()
            val port = viewPort
            if (port != null) {
                val useCaseGroup = UseCaseGroup.Builder()
                    .setViewPort(port)
                    .addUseCase(preview)
                    .addUseCase(imageAnalysis)
                    .build()
                cameraProviderFuture.get().bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    useCaseGroup
                )
            } else {
                cameraProviderFuture.get().bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageAnalysis
                )
            }
        }.onFailure {
            Log.e("CAMERA", "Camera bind error ${it.localizedMessage}", it)
        }
    }

    LaunchedEffect(camera, enableTorch) {
        camera.getOrNull()?.cameraControl?.enableTorch(enableTorch)
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
            preview.setSurfaceProvider(previewView.surfaceProvider)
            previewView.doOnLayout {
                viewPort = previewView.viewPort
            }
            previewView
        },
    )
}

fun CameraPosition.toSelector() = when(this) {
    CameraPosition.FRONT -> CameraSelector.LENS_FACING_FRONT
    CameraPosition.BACK -> CameraSelector.LENS_FACING_BACK
}
