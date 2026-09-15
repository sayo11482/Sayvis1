package com.example.sayvis.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.sayvis.ai.ConnectivityProbe
import com.example.sayvis.i18n.LocalStrings
import com.example.sayvis.identity.GoogleAccountHub
import com.example.sayvis.ui.SayvisViewModel
import com.example.sayvis.ui.components.ButtonTone
import com.example.sayvis.ui.components.SayvisButton
import com.example.sayvis.ui.components.SayvisCard
import com.example.sayvis.ui.components.SayvisSectionHeader
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.ui.theme.SayvisSurfaceVariant
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.concurrent.Executors

/**
 * Connect Centre (v4.0.0) — the single, simple place where the owner links
 * their Google account and sees the rebuilt network working:
 *
 *  - one tap «ادامه با اکانت گوگل» → system account sheet (zero config);
 *  - live connectivity verdict from the rebuilt SayvisNet layer;
 *  - pairing QR (readable by any computer/phone scanner) of the linked
 *    account + device + pin;
 *  - QR scanner that accepts a QR shown on a computer screen: another
 *    SAYVIS pairing payload, a provider API key or a config JSON;
 *  - a paste field as the no-camera fallback.
 */
@Composable
fun ConnectScreen(viewModel: SayvisViewModel) {
    val s = LocalStrings.current
    val settings by viewModel.settings.collectAsState()
    val connectivity by viewModel.connectivity.collectAsState()
    var pasteValue by remember { mutableStateOf("") }
    var importMessage by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshConnectivity() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("connect_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SayvisSectionHeader(title = s.connectHubTitle, icon = Icons.Default.Wifi)
        Text(text = s.connectHubHint, fontSize = 11.5.sp, color = SayvisSilverMuted)

        // ---------------------------------------------------- network state
        SayvisCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val result = connectivity
                    val dotColor = when (result?.state) {
                        ConnectivityProbe.NetState.ONLINE -> SayvisGreenSuccess
                        ConnectivityProbe.NetState.ONLINE_NO_GOOGLE -> SayvisAmberWarning
                        null, ConnectivityProbe.NetState.OFFLINE -> SayvisRedAlert
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = connectivity?.message(true) ?: s.connectNetChecking,
                            fontSize = 12.sp,
                            color = SayvisSilver,
                            modifier = Modifier.testTag("connect_net_state")
                        )
                        Text(text = s.connectNetNote, fontSize = 10.sp, color = SayvisSilverMuted)
                    }
                    SayvisButton(
                        label = s.connectNetRetry,
                        onClick = { viewModel.refreshConnectivity() },
                        tone = ButtonTone.NEUTRAL,
                        modifier = Modifier.testTag("connect_net_retry")
                    )
                }
            }
        }

        // ---------------------------------------------------- google account
        SayvisSectionHeader(title = s.connectAccountTitle, icon = Icons.Default.AccountCircle)
        SayvisCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val email = settings.google.email
                if (settings.google.signedIn) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SayvisSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = email.take(1).uppercase(),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = SayvisCyan
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f).testTag("connect_account_linked")) {
                            Text(
                                text = settings.google.displayName.ifBlank { email },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SayvisSilver
                            )
                            Text(text = email, fontSize = 11.sp, color = SayvisSilverMuted)
                            Text(text = s.connectAccountNote, fontSize = 10.sp, color = SayvisSilverMuted)
                        }
                    }
                    SayvisButton(
                        label = s.connectSignOut,
                        onClick = { viewModel.googleSignOut() },
                        tone = ButtonTone.DANGER,
                        modifier = Modifier.fillMaxWidth().testTag("connect_signout")
                    )
                } else {
                    Text(text = s.connectAccountHint, fontSize = 11.5.sp, color = SayvisSilver)
                    SayvisButton(
                        label = s.connectGoogleButton,
                        onClick = { viewModel.beginGoogleSignIn() },
                        tone = ButtonTone.PRIMARY,
                        modifier = Modifier.fillMaxWidth().testTag("connect_google")
                    )
                }
            }
        }

        // ------------------------------------------------------- pairing QR
        SayvisSectionHeader(title = s.connectQrTitle, icon = Icons.Default.QrCode2)
        SayvisCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = s.connectQrHint, fontSize = 11.sp, color = SayvisSilverMuted)
                val email = settings.google.email
                if (settings.google.signedIn) {
                    val payload = remember(email) {
                        runCatching { viewModel.pairingPayload() }.getOrDefault("")
                    }
                    val qrBitmap = remember(payload) { qrBitmap(payload, 560) }
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "pairing QR",
                            modifier = Modifier
                                .size(190.dp)
                                .testTag("connect_qr_image")
                        )
                    }
                    Text(
                        text = s.connectPinLabel + " " + GoogleAccountHub.devicePin(LocalContext.current),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SayvisSilver
                    )
                } else {
                    Text(text = s.connectQrNeedAccount, fontSize = 11.sp, color = SayvisSilverMuted)
                }
                SayvisButton(
                    label = s.connectScan,
                    onClick = { showScanner = true },
                    tone = ButtonTone.NEUTRAL,
                    modifier = Modifier.fillMaxWidth().testTag("connect_scan")
                )
                OutlinedTextField(
                    value = pasteValue,
                    onValueChange = { pasteValue = it },
                    singleLine = true,
                    placeholder = { Text(s.connectPasteHint, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("connect_paste")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SayvisButton(
                        label = s.connectImport,
                        onClick = {
                            if (pasteValue.isBlank()) {
                                importMessage = s.connectInvalid
                            } else {
                                val (ok, msg) = viewModel.handleQrPayload(pasteValue)
                                importMessage = if (ok) msg else "⚠️ " + msg
                                if (ok) pasteValue = ""
                            }
                        },
                        tone = ButtonTone.PRIMARY,
                        modifier = Modifier.weight(1f).testTag("connect_import")
                    )
                }
                if (importMessage.isNotBlank()) {
                    Text(
                        text = importMessage,
                        fontSize = 11.sp,
                        color = SayvisSilver,
                        modifier = Modifier.testTag("connect_import_result")
                    )
                }
            }
        }

        SayvisButton(
            label = s.connectLater,
            onClick = { viewModel.navigateTo(com.example.sayvis.ui.SayvisScreen.SETTINGS) },
            tone = ButtonTone.NEUTRAL,
            modifier = Modifier.fillMaxWidth().testTag("connect_later")
        )
    }

    if (showScanner) {
        QrScanSheet(
            onPayload = { raw ->
                val (ok, msg) = viewModel.handleQrPayload(raw)
                importMessage = if (ok) msg else "⚠️ " + msg
                showScanner = false
            },
            onClose = { showScanner = false }
        )
    }
}

/** Renders a QR bitmap for [text] with [pixelsPerModule] scale; null on overflow. */
internal fun qrBitmap(text: String, targetSize: Int = 560): Bitmap? = runCatching {
    val matrix = QRCodeWriter().encode(
        text, BarcodeFormat.QR_CODE, targetSize, targetSize,
        mapOf(EncodeHintType.MARGIN to 1)
    )
    val w = matrix.width
    val h = matrix.height
    val pixels = IntArray(w * h)
    for (y in 0 until h) {
        for (x in 0 until w) {
            pixels[y * w + x] = if (matrix.get(x, y)) 0xFF0A0B0E.toInt() else 0xFFF2F4F7.toInt()
        }
    }
    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, w, 0, 0, w, h)
    }
}.getOrNull()

/** Full-screen QR scanner: CameraX preview + on-device ML Kit barcode reader. */
@Composable
private fun QrScanSheet(
    onPayload: (String) -> Unit,
    onClose: () -> Unit
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCamera = it }
    var handled by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF20A0B0E))
            .testTag("connect_scanner")
    ) {
        if (hasCamera) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val future = ProcessCameraProvider.getInstance(ctx)
                    future.addListener({
                        val provider = future.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val scanner = BarcodeScanning.getClient()
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        val executor = Executors.newSingleThreadExecutor()
                        analysis.setAnalyzer(executor) { frame ->
                            processFrame(frame, scanner) { value ->
                                if (!handled) {
                                    handled = true
                                    executor.shutdown()
                                    previewView.post { onPayload(value) }
                                }
                            }
                        }
                        runCatching {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                ctx as androidx.lifecycle.LifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = s.connectScanPermission, fontSize = 12.sp, color = SayvisSilver)
                SayvisButton(
                    label = s.connectScanGrant,
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    tone = ButtonTone.PRIMARY,
                    modifier = Modifier.testTag("connect_scan_grant")
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SayvisButton(
                label = s.captureCancel,
                onClick = onClose,
                tone = ButtonTone.NEUTRAL,
                modifier = Modifier.testTag("connect_scan_close")
            )
            Text(
                text = s.connectScanHintLive,
                fontSize = 10.5.sp,
                color = SayvisSilverMuted,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

/** One ML Kit frame → first barcode raw value. */
private fun processFrame(
    frame: ImageProxy,
    scanner: BarcodeScanner,
    onValue: (String) -> Unit
) {
    val media = frame.image
    if (media == null) {
        frame.close(); return
    }
    val input = InputImage.fromMediaImage(media, frame.imageInfo.rotationDegrees)
    scanner.process(input)
        .addOnSuccessListener { barcodes ->
            val value = barcodes.firstOrNull()?.rawValue
            if (!value.isNullOrBlank()) onValue(value)
        }
        .addOnCompleteListener { frame.close() }
}
