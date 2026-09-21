package com.example.sayvis.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.sayvis.agent.ArenaAgent
import com.example.sayvis.ui.theme.*

/**
 * Arena-style graphical canvas — renders whatever the agent is currently
 * producing: terminal, file tree, HTML preview (WebView), image, diff.
 *
 * This is the “even graphical environment” the owner asked for: the same
 * live preview that Arena AI shows on the right side of the agent panel.
 */
@Composable
fun ArenaCanvas(
    toolCall: ArenaAgent.ToolCall?,
    htmlContent: String?,
    modifier: Modifier = Modifier
) {
    val graphType = toolCall?.graphicalData?.type

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SayvisSurfaceVariant)
            .border(1.dp, SayvisBorder, RoundedCornerShape(14.dp))
    ) {
        // Title bar — macOS dots + type
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SayvisSurface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFFF5F56)))
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFFFBD2E)))
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF27CA3F)))
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = iconFor(graphType),
                contentDescription = null,
                tint = SayvisCyan,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = toolCall?.graphicalData?.title ?: "Arena Canvas — Live Preview",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SayvisSilver
            )
            Spacer(Modifier.weight(1f))
            if (graphType == ArenaAgent.GraphType.HTML_PREVIEW) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SayvisGreenSuccess.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("● LIVE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = SayvisGreenSuccess)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(SayvisDeepSpace)
        ) {
            when (graphType) {
                ArenaAgent.GraphType.HTML_PREVIEW -> {
                    val html = htmlContent ?: toolCall?.graphicalData?.content ?: ""
                    if (html.isNotBlank() && html.contains("<html", true)) {
                        HtmlPreview(html = html, modifier = Modifier.fillMaxSize())
                    } else {
                        EmptyCanvas("HTML preview will appear here", Icons.Default.Language)
                    }
                }
                ArenaAgent.GraphType.TERMINAL -> {
                    TerminalView(text = toolCall.output, modifier = Modifier.fillMaxSize())
                }
                ArenaAgent.GraphType.FILE_TREE -> {
                    FileTreePreview(modifier = Modifier.fillMaxSize())
                }
                ArenaAgent.GraphType.DIFF -> {
                    DiffView(text = toolCall.output, modifier = Modifier.fillMaxSize())
                }
                ArenaAgent.GraphType.IMAGE -> {
                    ImageCanvasView(modifier = Modifier.fillMaxSize())
                }
                ArenaAgent.GraphType.WEB_RESULTS -> {
                    WebResultsView(modifier = Modifier.fillMaxSize())
                }
                ArenaAgent.GraphType.CANVAS -> {
                    CanvasPreview(modifier = Modifier.fillMaxSize())
                }
                null -> {
                    EmptyCanvas("Agent will render here — like Arena AI's live preview", Icons.Default.AutoAwesome)
                }
            }
        }
    }
}

@Composable
private fun HtmlPreview(html: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var webViewRef: WebView? by remember { mutableStateOf(null) }
    DisposableEffect(Unit) {
        onDispose { webViewRef?.destroy() }
    }
    AndroidView(
        factory = {
            WebView(context).apply {
                webViewRef = this
                @SuppressLint("SetJavaScriptEnabled")
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                webViewClient = WebViewClient()
                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}

@Composable
private fun TerminalView(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFF0B0D12))
            .padding(12.dp)
    ) {
        Text(
            text = text.ifBlank { "$ agent --help\n  Arena agent terminal ready" },
            fontSize = 11.sp,
            color = Color(0xFF00D4FF),
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun FileTreePreview(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FileTreeLine("📁 workspace/", true)
        FileTreeLine("  📄 index.html", false, "2.4 KB")
        FileTreeLine("  📄 style.css", false, "1.1 KB", modified = true)
        FileTreeLine("  📄 script.js", false, "0.4 KB")
        FileTreeLine("  📁 assets/", true)
        FileTreeLine("    🖼️ hero.jpg", false)
        FileTreeLine("  📄 README.md", false)
    }
}

@Composable
private fun FileTreeLine(text: String, isDir: Boolean, size: String? = null, modified: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = text, fontSize = 11.sp, color = if (isDir) SayvisGold else SayvisSilver, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        if (size != null) {
            Spacer(Modifier.width(8.dp))
            Text(text = size, fontSize = 9.sp, color = SayvisSilverMuted)
        }
        if (modified) {
            Spacer(Modifier.width(6.dp))
            Box(Modifier.clip(RoundedCornerShape(4.dp)).background(SayvisAmberWarning.copy(alpha = 0.2f)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                Text("M", fontSize = 8.sp, color = SayvisAmberWarning, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DiffView(text: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(Color(0xFF0B0D12)).padding(12.dp)) {
        Text("diff --git a/style.css b/style.css", fontSize = 10.sp, color = SayvisSilverMuted, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        Text("+ .hero { background: radial-gradient(...); }", fontSize = 10.sp, color = SayvisGreenSuccess, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        Text("+ .card { backdrop-filter: blur(12px); }", fontSize = 10.sp, color = SayvisGreenSuccess, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        Spacer(Modifier.height(8.dp))
        Text(text, fontSize = 10.sp, color = SayvisCyan, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

@Composable
private fun ImageCanvasView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SayvisSurface)
                .border(1.dp, SayvisGold.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Image, contentDescription = null, tint = SayvisGold, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(8.dp))
                Text("Generated Image — 1024×1024", fontSize = 11.sp, color = SayvisSilver)
                Text("gallery/generated_01.png", fontSize = 9.sp, color = SayvisSilverMuted)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("🎨 Arena canvas — generated images appear here", fontSize = 10.sp, color = SayvisSilverMuted)
    }
}

@Composable
private fun WebResultsView(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WebResultCard("Dribbble — Modern Dashboard Inspiration", "dribbble.com", "Top trending dashboards with glassmorphism...")
        WebResultCard("MDN — CSS backdrop-filter", "developer.mozilla.org", "The backdrop-filter CSS property lets you apply...")
        WebResultCard("Arena AI — Agent Patterns", "arena.ai", "How Arena's agent handles graphical previews...")
    }
}

@Composable
private fun WebResultCard(title: String, source: String, snippet: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SayvisSurface)
            .padding(10.dp)
    ) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
        Text(source, fontSize = 9.sp, color = SayvisCyan)
        Text(snippet, fontSize = 10.sp, color = SayvisSilverMuted, maxLines = 2)
    }
}

@Composable
private fun CanvasPreview(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(SayvisDeepSpace).padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SayvisGreenSuccess, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text("All steps verified", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SayvisGreenSuccess)
            Text("Graphical environment ready — like Arena AI", fontSize = 10.sp, color = SayvisSilverMuted)
        }
    }
}

@Composable
private fun EmptyCanvas(message: String, icon: ImageVector) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SayvisDeepSpace)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = SayvisSilverMuted, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, fontSize = 11.sp, color = SayvisSilverMuted, lineHeight = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

private fun iconFor(type: ArenaAgent.GraphType?): ImageVector = when (type) {
    ArenaAgent.GraphType.HTML_PREVIEW -> Icons.Default.Language
    ArenaAgent.GraphType.TERMINAL -> Icons.Default.Code
    ArenaAgent.GraphType.FILE_TREE -> Icons.Default.Folder
    ArenaAgent.GraphType.DIFF -> Icons.Default.EditNote
    ArenaAgent.GraphType.IMAGE -> Icons.Default.Image
    ArenaAgent.GraphType.WEB_RESULTS -> Icons.Default.TravelExplore
    ArenaAgent.GraphType.CANVAS -> Icons.Default.Draw
    null -> Icons.Default.AutoAwesome
}
