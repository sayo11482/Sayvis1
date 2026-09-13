package com.example.sayvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.model.MemoryItem
import com.example.sayvis.model.MemoryType
import com.example.sayvis.ui.theme.SayvisBorder
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.ui.theme.SayvisSurfaceVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Self-learning memory browser: everything SAYVIS remembers from its conversations
 * with the owner. The owner remains sovereign - any memory can be deleted.
 */
@Composable
fun MemoryScreen(
    memories: List<MemoryItem>,
    isPersian: Boolean,
    onDeleteMemory: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("memory_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPersian) "حافظه‌ی سایویس" else "SAYVIS Memory",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SayvisGold
                )
                Text(
                    text = if (isPersian)
                        "${memories.size} خاطره — هر چیزی که از گفتگوهای شما یاد گرفته‌ام"
                    else
                        "${memories.size} memories - everything I have learned from our conversations",
                    fontSize = 11.sp,
                    color = SayvisSilverMuted
                )
            }
            if (memories.isNotEmpty()) {
                OutlinedButton(onClick = onClearAll) {
                    Text(if (isPersian) "پاک کردن همه" else "Clear all", fontSize = 11.sp, color = SayvisSilver)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (memories.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SayvisSurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = null,
                    tint = SayvisSilverMuted,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isPersian)
                        "هنوز چیزی یاد نگرفته‌ام.\nبا من در تب «هوش سایو» گفتگو کن — هر مکالمه به حافظه‌ام اضافه می‌شود و جواب‌های بعدی‌ام شخصی‌تر می‌شود."
                    else
                        "I have not learned anything yet.\nChat with me in the AI tab - every conversation is stored and makes my future answers more personal.",
                    fontSize = 12.sp,
                    color = SayvisSilverMuted,
                    lineHeight = 18.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(memories, key = { it.id }) { memory ->
                    MemoryCard(
                        memory = memory,
                        dateText = dateFormat.format(Date(memory.createdAt)),
                        isPersian = isPersian,
                        onDelete = { onDeleteMemory(memory.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun MemoryCard(
    memory: MemoryItem,
    dateText: String,
    isPersian: Boolean,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SayvisSurfaceVariant)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = memory.type.labelFa.takeIf { isPersian } ?: memory.type.labelEn,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SayvisCyan,
                modifier = Modifier
                    .background(SayvisCyan.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = dateText,
                fontSize = 10.sp,
                color = SayvisSilverMuted,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = if (isPersian) "حذف" else "Delete",
                    tint = SayvisSilverMuted,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = memory.content,
            fontSize = 12.sp,
            color = Color.White,
            lineHeight = 18.sp
        )
        if (memory.source.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "via ${memory.source} • ${memory.epistemicStatus.labelEn}",
                fontSize = 9.sp,
                color = SayvisBorder
            )
        }
    }
}
