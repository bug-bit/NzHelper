package me.neko.nzhelper.feature.about

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OpenSourceScreen(
    navController: NavHostController
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("开放源代码") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright)
                ) {
                    Column {
                        licenseList.forEachIndexed { index, item ->
                            LicenseItemView(item) {
                                val intent = Intent(Intent.ACTION_VIEW, item.url.toUri())
                                context.startActivity(intent)
                            }
                            if (index < licenseList.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LicenseItemView(
    item: LicenseItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Code,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${item.name} - ${item.author}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = item.url,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            )

            Spacer(Modifier.height(6.dp))

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = getLicenseShort(item.type),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

private fun getLicenseShort(type: LicenseType): String =
    when (type) {
        LicenseType.Apache2 -> "Apache 2.0"
        LicenseType.MIT -> "MIT"
        LicenseType.GPL3 -> "GPL 3.0"
    }

private val licenseList = listOf(
    LicenseItem(
        "Google",
        "Activity Compose",
        "https://developer.android.com/jetpack/androidx/releases/activity",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "androidx.compose.material.icons",
        "https://developer.android.com/reference/kotlin/androidx/compose/material/icons/package-summary",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "androidx.compose.material3.windowsizeclass",
        "https://developer.android.com/reference/kotlin/androidx/compose/material3/windowsizeclass/package-summary",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "androidx.compose.ui.graphics",
        "https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/package-summary",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "androidx.compose.ui.tooling",
        "https://developer.android.com/reference/kotlin/androidx/compose/ui/tooling/package-summary",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "androidx.compose.ui.tooling.preview",
        "https://developer.android.com/reference/kotlin/androidx/compose/ui/tooling/preview/package-summary",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "Compose Material 3",
        "https://developer.android.com/jetpack/androidx/releases/compose-material3",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "Compose Material 3 Adaptive",
        "https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "Core",
        "https://developer.android.com/jetpack/androidx/releases/core",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "Gson",
        "https://github.com/google/gson",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "Jetpack Compose",
        "https://github.com/androidx/androidx",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "Lifecycle",
        "https://developer.android.com/jetpack/androidx/releases/lifecycle",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "Material Design 3",
        "https://m3.material.io/",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "Navigation with Compose",
        "https://developer.android.com/develop/ui/compose/navigation",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "Test",
        "https://developer.android.com/jetpack/androidx/releases/test",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "Use a Bill of Materials",
        "https://developer.android.com/develop/ui/compose/bom",
        LicenseType.Apache2
    ),
    LicenseItem(
        "JetBrains",
        "Kotlin",
        "https://github.com/JetBrains/kotlin",
        LicenseType.Apache2
    ),
    LicenseItem(
        "square",
        "Moshi",
        "https://github.com/square/moshi",
        LicenseType.Apache2
    ),
    LicenseItem(
        "square",
        "okhttp",
        "https://github.com/square/okhttp",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "Biometric",
        "https://developer.android.com/jetpack/androidx/releases/biometric",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "Appcompat",
        "https://developer.android.com/jetpack/androidx/releases/appcompat",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "WorkManager",
        "https://developer.android.com/jetpack/androidx/releases/work",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "Room",
        "https://developer.android.com/jetpack/androidx/releases/room",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "Kotlin Symbol Processing API",
        "https://github.com/google/ksp",
        LicenseType.Apache2
    )
)

data class LicenseItem(
    val author: String,
    val name: String,
    val url: String,
    val type: LicenseType
)

enum class LicenseType {
    Apache2,
    MIT,
    GPL3
}

@Preview(showBackground = true)
@Composable
fun OpenSourceScreenPreview() {
    OpenSourceScreen(
        navController = rememberNavController()
    )
}