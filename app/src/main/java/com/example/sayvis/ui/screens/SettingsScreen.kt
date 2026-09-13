package com.example.sayvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.core.SettingsStore
import com.example.sayvis.ui.SayvisViewModel
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisBorder
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.ui.theme.SayvisSurface
import com.example.sayvis.ui.theme.SayvisSurfaceVariant

/**
 * Runtime settings: cloud AI keys, models and provider routing.
 * Everything is stored locally on-device and applied immediately (no rebuild needed).
 */
@Composable
fun SettingsScreen(
    viewModel: SayvisViewModel,
    isPersian: Boolean,
    modifier: Modifier = Modifier
) {
    val connectionStatus by viewModel.connectionTestStatus.collectAsState()

    var providerChoice by remember { mutableStateOf(SettingsStore.providerChoice) }
    var geminiKey by remember { mutableStateOf(SettingsStore.geminiKey) }
    var openRouterKey by remember { mutableStateOf(SettingsStore.openRouterKey) }
    var groqKey by remember { mutableStateOf(SettingsStore.groqKey) }
    var geminiModel by remember { mutableStateOf(SettingsStore.geminiModel) }
    var openRouterModel by remember { mutableStateOf(SettingsStore.openRouterModel) }
    var groqModel by remember { mutableStateOf(SettingsStore.groqModel) }

    val choices = listOf(
        SettingsStore.PROVIDER_AUTO to (if (isPersian) "خودکار" else "Auto"),
        SettingsStore.PROVIDER_GEMINI to "Gemini",
        SettingsStore.PROVIDER_OPENROUTER to "OpenRouter",
        SettingsStore.PROVIDER_GROQ to "Groq",
        SettingsStore.PROVIDER_LOCAL to (if (isPersian) "فقط محلی" else "Local")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        Text(
            text = if (isPersian) "هسته‌ی هوش مصنوعی" else "AI Core",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SayvisCyan
        )
        Text(
            text = if (isPersian)
                "کلیدهای API فقط روی همین گوشی ذخیره می‌شوند و مستقیم به سرویس انتخابی شما می‌روند."
            else
                "API keys are stored on-device only and sent directly to the provider you choose.",
            fontSize = 11.sp,
            color = SayvisSilverMuted
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Provider routing
        Text(
            text = if (isPersian) "انتخاب سرویس" else "Provider routing",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = SayvisSilver
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            choices.forEach { (id, label) ->
                val selected = providerChoice == id
                if (selected) {
                    Button(
                        onClick = {
                            providerChoice = id
                            viewModel.setProviderChoice(id)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SayvisCyan, contentColor = androidx.compose.ui.graphics.Color.Black)
                    ) { Text(label, fontSize = 11.sp) }
                } else {
                    OutlinedButton(onClick = {
                        providerChoice = id
                        viewModel.setProviderChoice(id)
                    }) { Text(label, fontSize = 11.sp, color = SayvisSilver) }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // API keys
        SettingsKeyField(
            label = "Google Gemini API Key",
            hint = "free at aistudio.google.com/apikey",
            value = geminiKey,
            onChange = { geminiKey = it; viewModel.setGeminiKey(it) }
        )
        SettingsModelField(
            label = if (isPersian) "مدل Gemini" else "Gemini model",
            value = geminiModel,
            placeholder = SettingsStore.DEFAULT_GEMINI_MODEL,
            onChange = { geminiModel = it; viewModel.setGeminiModel(it) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        SettingsKeyField(
            label = "OpenRouter API Key",
            hint = "openrouter.ai/keys",
            value = openRouterKey,
            onChange = { openRouterKey = it; viewModel.setOpenRouterKey(it) }
        )
        SettingsModelField(
            label = if (isPersian) "مدل OpenRouter" else "OpenRouter model",
            value = openRouterModel,
            placeholder = SettingsStore.DEFAULT_OPENROUTER_MODEL,
            onChange = { openRouterModel = it; viewModel.setOpenRouterModel(it) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        SettingsKeyField(
            label = "Groq API Key",
            hint = "console.groq.com/keys",
            value = groqKey,
            onChange = { groqKey = it; viewModel.setGroqKey(it) }
        )
        SettingsModelField(
            label = if (isPersian) "مدل Groq" else "Groq model",
            value = groqModel,
            placeholder = SettingsStore.DEFAULT_GROQ_MODEL,
            onChange = { groqModel = it; viewModel.setGroqModel(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.testCloudConnection() },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_test_connection"),
            colors = ButtonDefaults.buttonColors(containerColor = SayvisGreenSuccess, contentColor = androidx.compose.ui.graphics.Color.Black)
        ) {
            Text(
                if (isPersian) "تست اتصال به مغز ابری" else "Test cloud connection",
                fontWeight = FontWeight.Bold
            )
        }

        if (connectionStatus != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = connectionStatus ?: "",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = if (connectionStatus?.startsWith("✅") == true) SayvisGreenSuccess else SayvisAmberWarning
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Help card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SayvisSurfaceVariant, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Text(
                text = if (isPersian) "چطور کلید رایگان بگیرم؟" else "How to get a free key?",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SayvisCyan
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isPersian)
                    "۱. Gemini: در مرورگر به aistudio.google.com/apikey بروید، با اکانت گوگل وارد شوید و «Create API key» بزنید (رایگان).\n۲. کلید را کپی و در فیلد بالا جای‌گذاری کنید.\n۳. دکمه‌ی تست اتصال را بزنید — باید ✅ ببینید.\n\nOpenRouter و Groq هم کلید رایگان می‌دهند اگر مدل‌های بیشتری خواستید."
                else
                    "1. Gemini: open aistudio.google.com/apikey in a browser, sign in with Google and click \"Create API key\" (free tier available).\n2. Paste the key into the field above.\n3. Tap \"Test cloud connection\" - you should see ✅.\n\nOpenRouter and Groq also offer free keys if you want more models.",
                fontSize = 11.sp,
                color = SayvisSilver,
                lineHeight = 17.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsKeyField(
    label: String,
    hint: String,
    value: String,
    onChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label, fontSize = 12.sp) },
            placeholder = { Text(hint, fontSize = 11.sp, color = SayvisSilverMuted) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_key_field"),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SayvisCyan,
                unfocusedBorderColor = SayvisBorder,
                focusedContainerColor = SayvisSurfaceVariant,
                unfocusedContainerColor = SayvisSurfaceVariant
            )
        )
    }
}

@Composable
private fun SettingsModelField(
    label: String,
    value: String,
    placeholder: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 11.sp) },
        placeholder = { Text(placeholder, fontSize = 11.sp, color = SayvisSilverMuted) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SayvisCyan,
            unfocusedBorderColor = SayvisBorder,
            focusedContainerColor = SayvisSurface,
            unfocusedContainerColor = SayvisSurface
        )
    )
}
