package com.example.sayvis.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.i18n.LocalStrings
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisDeepSpace
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.voice.AvatarListenController

/**
 * First-launch permission gate: the device explicitly asks the owner to approve
 * every capability — microphone, camera, location, gallery, contacts and the
 * listening notification — with a bilingual explanation of what each one is for.
 * Nothing is granted silently and "continue" always remains available.
 */
@Composable
fun PermissionsGate(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val context = LocalContext.current

    var cameraOk by remember { mutableStateOf(granted(context, Manifest.permission.CAMERA)) }
    var micOk by remember { mutableStateOf(granted(context, Manifest.permission.RECORD_AUDIO)) }
    var locationOk by remember {
        mutableStateOf(
            granted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                granted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }
    var galleryOk by remember { mutableStateOf(granted(context, galleryPermission())) }
    var contactsOk by remember { mutableStateOf(granted(context, Manifest.permission.READ_CONTACTS)) }
    var notifOk by remember { mutableStateOf(AvatarListenController.hasNotificationPermission(context)) }
    var overlayOk by remember { mutableStateOf(AvatarListenController.hasOverlayPermission(context)) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { cameraOk = it }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { micOk = it }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        locationOk = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { galleryOk = it }
    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { contactsOk = it }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifOk = it }

    val allLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        cameraOk = result[Manifest.permission.CAMERA] == true || cameraOk
        micOk = result[Manifest.permission.RECORD_AUDIO] == true || micOk
        locationOk = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true || locationOk
        galleryOk = result[galleryPermission()] == true || galleryOk
        contactsOk = result[Manifest.permission.READ_CONTACTS] == true || contactsOk
        notifOk = result[Manifest.permission.POST_NOTIFICATIONS] == true || notifOk || Build.VERSION.SDK_INT < 33
    }

    fun missing(): Array<String> = buildList {
        if (!micOk) add(Manifest.permission.RECORD_AUDIO)
        if (!cameraOk) add(Manifest.permission.CAMERA)
        if (!locationOk) {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (!galleryOk) add(galleryPermission())
        if (!contactsOk) add(Manifest.permission.READ_CONTACTS)
        if (!notifOk && Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SayvisDeepSpace)
            .testTag("permissions_gate")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(42.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = SayvisCyan, modifier = Modifier.size(30.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = s.gateTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
                    Text(text = s.gateSubtitle, fontSize = 11.sp, color = SayvisSilverMuted, lineHeight = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            GateRow(icon = Icons.Default.Mic, tint = SayvisCyan, title = s.permMic, desc = s.permMicDesc, granted = micOk)
            GateRow(icon = Icons.Default.CameraAlt, tint = SayvisGold, title = s.permCamera, desc = s.permCameraDesc, granted = cameraOk)
            GateRow(icon = Icons.Default.LocationOn, tint = SayvisGreenSuccess, title = s.permLocation, desc = s.permLocationDesc, granted = locationOk)
            GateRow(icon = Icons.Default.Image, tint = SayvisGold, title = s.permGallery, desc = s.permGalleryDesc, granted = galleryOk)
            GateRow(icon = Icons.Default.Contacts, tint = SayvisCyan, title = s.permContacts, desc = s.permContactsDesc, granted = contactsOk)
            GateRow(icon = Icons.Default.Notifications, tint = SayvisGreenSuccess, title = s.permNotif, desc = s.permNotifDesc, granted = notifOk)
            GateRow(icon = Icons.Default.Security, tint = SayvisGold, title = s.permOverlay, desc = s.permOverlayDesc, granted = overlayOk) {
                context.startActivity(AvatarListenController.overlaySettingsIntent(context))
            }

            Spacer(modifier = Modifier.height(20.dp))

            SayvisButton(
                label = s.gateGrantAll,
                onClick = {
                    val missingPerms = missing()
                    if (missingPerms.isNotEmpty()) {
                        allLauncher.launch(missingPerms)
                    }
                    if (!overlayOk) {
                        context.startActivity(AvatarListenController.overlaySettingsIntent(context))
                    }
                },
                tone = ButtonTone.PRIMARY,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gate_grant_all")
            )
            Spacer(modifier = Modifier.height(8.dp))
            SayvisButton(
                label = s.gateContinue,
                onClick = onFinish,
                tone = ButtonTone.NEUTRAL,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gate_continue")
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = s.gatePrivacyNote,
                fontSize = 10.5.sp,
                color = SayvisSilverMuted.copy(alpha = 0.8f),
                lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun GateRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    desc: String,
    granted: Boolean,
    onClick: (() -> Unit)? = null
) {
    val s = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
            Text(text = desc, fontSize = 10.5.sp, color = SayvisSilverMuted, lineHeight = 13.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (granted) {
            androidx.compose.material3.Text(
                text = s.avatarPermGranted,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = SayvisGreenSuccess
            )
        } else {
            SayvisButton(
                label = s.avatarPermGrant,
                onClick = { onClick?.invoke() },
                tone = ButtonTone.GOLD
            )
        }
    }
}

private fun granted(context: android.content.Context, permission: String): Boolean =
    androidx.core.content.ContextCompat.checkSelfPermission(context, permission) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

/** Public check used by the Settings permission card. */
fun checkPermission(context: android.content.Context, permission: String): Boolean = granted(context, permission)

/** Gallery read: scoped media permission on 33+, classic storage below. */
fun galleryPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
    else Manifest.permission.READ_EXTERNAL_STORAGE

/** All runtime permissions the owner gate manages, for the Settings status card. */
fun allRuntimePermissions(): List<Triple<String, String, Boolean>> = buildList {
    add(Triple(Manifest.permission.RECORD_AUDIO, "mic", false))
    add(Triple(Manifest.permission.CAMERA, "camera", false))
    add(Triple(Manifest.permission.ACCESS_FINE_LOCATION, "location", true))
    add(Triple(galleryPermission(), "gallery", false))
    add(Triple(Manifest.permission.READ_CONTACTS, "contacts", false))
    if (Build.VERSION.SDK_INT >= 33) add(Triple(Manifest.permission.POST_NOTIFICATIONS, "notifications", false))
}
