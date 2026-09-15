package com.example.sayvis.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import com.example.sayvis.i18n.LocalStrings
import com.example.sayvis.ui.components.aiStyleColor
import com.example.sayvis.ui.theme.SayvisDeepSpace
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Digital Mirror: SAYVIS watches through the selfie camera (on-device ML
 * Kit face landmarks — no cloud, no photo ever leaves the phone) and repaints
 * the owner as a living robotic portrait: a hexagonal mesh anchored on the
 * real face bounds, angular logo-style eyes on the true eye positions, a
 * segmented mouth on the real mouth line, cheek nodes, and an orbiting
 * electron. The palette cycles with the atomic style; a scan line sweeps
 * while the face is being tracked.
 */
/** Normalised snapshot of the best tracked face (analysis-space → 0..1). */
data class FaceSnapshot(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val leftEye: Offset?,
    val rightEye: Offset?,
    val nose: Offset?,
    val mouthLeft: Offset?,
    val mouthRight: Offset?,
    val mouthBottom: Offset?,
    val leftCheek: Offset?,
    val rightCheek: Offset?
) {
    val hasFace: Boolean get() = right > left && bottom > top
}

@Composable
fun DigitalMirrorScreen(modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    var face by remember { mutableStateOf<FaceSnapshot?>(null) }

    val detector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()
        )
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val analysisImageWidth = remember { mutableStateOf(1f) }
    val analysisImageHeight = remember { mutableStateOf(1f) }

    DisposableEffect(Unit) {
        onCleanup {
            detector.close()
            executor.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            val provider = ProcessCameraProvider.getInstance(context).get()
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy: ImageProxy ->
                val media = proxy.image
                if (media == null) {
                    proxy.close()
                    return@setAnalyzer
                }
                analysisImageWidth.value = proxy.width.toFloat()
                analysisImageHeight.value = proxy.height.toFloat()
                val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                detector.process(input)
                    .addOnSuccessListener { faces ->
                        face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                            ?.toSnapshot()
                    }
                    .addOnCompleteListener { proxy.close() }
            }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, analysis)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SayvisDeepSpace)
            .testTag("mirror_screen")
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SAYVIS // MIRROR",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                color = SayvisSilverMuted
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (face?.hasFace == true) "● TRACKING" else "○ SEARCH",
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
                color = if (face?.hasFace == true) SayvisGold else SayvisSilverMuted,
                modifier = Modifier.testTag("mirror_tracking")
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            RoboticPortraitCanvas(
                face = face,
                frameWidth = analysisImageWidth.value,
                frameHeight = analysisImageHeight.value,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .testTag("mirror_canvas")
            )
            if (face?.hasFace != true) {
                Text(
                    text = s.mirrorHint,
                    fontSize = 12.sp,
                    color = SayvisSilverMuted,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 26.dp)
                        .testTag("mirror_hint")
                )
            }
        }
    }
}

private fun Face.toSnapshot(): FaceSnapshot {
    val box = boundingBox
    fun mark(type: Int): Offset? =
        getLandmark(type)?.let { Offset(it.position.x.toFloat(), it.position.y.toFloat()) }
    return FaceSnapshot(
        left = box.left.toFloat(),
        top = box.top.toFloat(),
        right = box.right.toFloat(),
        bottom = box.bottom.toFloat(),
        leftEye = mark(FaceLandmark.LEFT_EYE),
        rightEye = mark(FaceLandmark.RIGHT_EYE),
        nose = mark(FaceLandmark.NOSE_BASE),
        mouthLeft = mark(FaceLandmark.MOUTH_LEFT),
        mouthRight = mark(FaceLandmark.MOUTH_RIGHT),
        mouthBottom = mark(FaceLandmark.MOUTH_BOTTOM),
        leftCheek = mark(FaceLandmark.LEFT_CHEEK),
        rightCheek = mark(FaceLandmark.RIGHT_CHEEK)
    )
}

@Composable
private fun RoboticPortraitCanvas(
    face: FaceSnapshot?,
    frameWidth: Float,
    frameHeight: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "mirror")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 5200, easing = LinearEasing)),
        label = "mirrorPhase"
    )

    Canvas(modifier) {
        val d = density
        fun px(dp: Float): Float = dp * d
        val core = aiStyleColor(phase)
        val cx = size.width / 2f
        val cy = size.height / 2f

        // faint backdrop grid
        val grid = px(26f)
        var gx = 0f
        while (gx < size.width) {
            drawLine(Color(0xFF16233A).copy(alpha = 0.35f), Offset(gx, 0f), Offset(gx, size.height), 1f)
            gx += grid
        }
        var gy = 0f
        while (gy < size.height) {
            drawLine(Color(0xFF16233A).copy(alpha = 0.35f), Offset(0f, gy), Offset(size.width, gy), 1f)
            gy += grid
        }

        val snapshot = face
        if (snapshot == null || !snapshot.hasFace || frameWidth <= 1f) {
            // Idle: a slow breathing ring waiting for a face.
            val r = size.minDimension * (0.22f + 0.01f * sin(phase * 2f * PI.toFloat()))
            drawCircle(core.copy(alpha = 0.25f), r, Offset(cx, cy), style = Stroke(px(1.4f)))
            return@Canvas
        }

        // Map analysis coords → canvas coords (letterboxed fit).
        val scale = minOf(size.width / frameWidth, size.height / frameHeight)
        val offX = (size.width - frameWidth * scale) / 2f
        val offY = (size.height - frameHeight * scale) / 2f
        fun px2x(x: Float): Float = offX + x * scale
        fun px2y(y: Float): Float = offY + y * scale

        val fl = px2x(snapshot.left)
        val ft = px2y(snapshot.top)
        val fr = px2x(snapshot.right)
        val fb = px2y(snapshot.bottom)
        val fw = fr - fl
        val fh = fb - ft

        // --- face frame: rounded robotic mask
        drawRoundRect(
            color = core.copy(alpha = 0.55f),
            topLeft = Offset(fl, ft),
            size = Size(fw, fh),
            cornerRadius = CornerRadius(fw * 0.30f),
            style = Stroke(px(1.6f))
        )
        // inner hex mesh: three nested rounded frames
        for (k in 1..2) {
            val inset = k * fw * 0.09f
            drawRoundRect(
                color = core.copy(alpha = 0.22f - 0.06f * k),
                topLeft = Offset(fl + inset, ft + inset),
                size = Size(fw - 2 * inset, fh - 2 * inset),
                cornerRadius = CornerRadius(fw * 0.26f),
                style = Stroke(1f)
            )
        }

        // --- eyes: angular logo-style on the real landmarks
        val eyeL = snapshot.leftEye?.let { Offset(px2x(it.x), px2y(it.y)) }
        val eyeR = snapshot.rightEye?.let { Offset(px2x(it.x), px2y(it.y)) }
        val eyeW = fw * 0.16f
        val eyeH = fh * 0.045f
        fun eye(center: Offset?) {
            if (center == null) return
            drawRoundRect(
                color = core,
                topLeft = Offset(center.x - eyeW / 2f, center.y - eyeH / 2f),
                size = Size(eyeW, eyeH),
                cornerRadius = CornerRadius(eyeH / 2f)
            )
            drawCircle(Color(0xFF05090F), eyeH * 0.28f, Offset(center.x + eyeW * 0.30f, center.y))
        }
        eye(eyeL)
        eye(eyeR)

        // --- nose tick
        snapshot.nose?.let {
            val n = Offset(px2x(it.x), px2y(it.y))
            drawLine(core.copy(alpha = 0.7f), Offset(n.x, n.y - fh * 0.02f), Offset(n.x, n.y), px(1.4f), StrokeCap.Round)
        }

        // --- mouth: segmented line through the real mouth landmarks
        val mL = snapshot.mouthLeft?.let { Offset(px2x(it.x), px2y(it.y)) }
        val mR = snapshot.mouthRight?.let { Offset(px2x(it.x), px2y(it.y)) }
        val mB = snapshot.mouthBottom?.let { Offset(px2x(it.x), px2y(it.y)) }
        if (mL != null && mR != null) {
            val segments = 5
            val sag = (mB?.y?.minus(mL.y))?.coerceAtLeast(0f) ?: fh * 0.03f
            for (i in 0 until segments) {
                val t0 = i / segments.toFloat()
                val t1 = (i + 1) / segments.toFloat()
                fun at(t: Float, drop: Float): Offset =
                    Offset(mL.x + (mR.x - mL.x) * t, mL.y + (mR.y - mL.y) * t + drop * sag)
                drawLine(
                    core.copy(alpha = 0.85f),
                    at(t0, sin((t0 + 0.05f) * PI).toFloat()),
                    at(t1, sin((t1 - 0.05f) * PI).toFloat()),
                    px(2f),
                    StrokeCap.Round
                )
            }
        }

        // --- cheek nodes
        listOfNotNull(snapshot.leftCheek, snapshot.rightCheek).forEach { cheek ->
            val c = Offset(px2x(cheek.x), px2y(cheek.y))
            drawCircle(core.copy(alpha = 0.5f), px(3f), c)
        }

        // --- scan line sweeping the face frame
        val sweep = (phase * 2f) % 1f
        val sy = ft + sweep * fh
        drawLine(core.copy(alpha = 0.30f), Offset(fl, sy), Offset(fr, sy), px(1.2f))

        // --- orbiting electron around the mask
        val ang = phase * 2f * PI.toFloat()
        val orbit = Offset(
            fl + fw / 2f + (fw * 0.78f) * cos(ang),
            ft + fh / 2f + (fh * 0.72f) * sin(ang)
        )
        drawCircle(SayvisGold.copy(alpha = 0.8f), px(2.6f), orbit)
    }
}
