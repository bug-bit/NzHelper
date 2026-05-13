package me.neko.nzhelper.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import me.neko.nzhelper.data.CustomOptions
import me.neko.nzhelper.data.SessionRepository
import me.neko.nzhelper.ui.dialog.CustomOptionsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var customOptions by remember { mutableStateOf<CustomOptions?>(null) }
    var showMoodDialog by remember { mutableStateOf(false) }
    var showPropsDialog by remember { mutableStateOf(false) }
    
    // 菜单和导入导出
    var showMenu by remember { mutableStateOf(false) }
    
    val importOptionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        uri?.let { importUri ->
            scope.launch {
                try {
                    context.contentResolver.openInputStream(importUri)?.use { inputStream ->
                        val jsonStr = inputStream.bufferedReader().readText()
                        val importedOptions = com.google.gson.Gson().fromJson(
                            jsonStr,
                            object : com.google.gson.reflect.TypeToken<CustomOptions>() {}.type
                        ) as? CustomOptions
                        
                        if (importedOptions != null) {
                            SessionRepository.saveCustomOptions(context, importedOptions)
                            customOptions = importedOptions
                            Toast.makeText(context, "成功导入自定义选项", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "导入失败：文件格式不正确", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "导入失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val exportOptionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: android.net.Uri? ->
        uri?.let { exportUri ->
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(exportUri)?.use { outputStream ->
                        val json = com.google.gson.Gson().toJson(customOptions ?: CustomOptions())
                        outputStream.write(json.toByteArray())
                        Toast.makeText(context, "成功导出自定义选项", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 加载自定义选项
    LaunchedEffect(Unit) {
        customOptions = SessionRepository.loadCustomOptions(context)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("设置") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "菜单")
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("导入自定义选项") },
                            onClick = {
                                showMenu = false
                                importOptionsLauncher.launch(arrayOf("application/json"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("导出自定义选项") },
                            onClick = {
                                showMenu = false
                                exportOptionsLauncher.launch("custom_options.json")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("重置为默认值") },
                            onClick = {
                                showMenu = false
                                scope.launch {
                                    val defaultOptions = CustomOptions()
                                    SessionRepository.saveCustomOptions(context, defaultOptions)
                                    customOptions = defaultOptions
                                    Toast.makeText(context, "已重置为默认选项", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMoodDialog = true },
                headlineContent = {
                    Text(text = "添加更多心情", style = MaterialTheme.typography.titleMedium)
                },
                supportingContent = {
                    customOptions?.let { options ->
                        Text(text = "当前：${options.moods.joinToString("、")}")
                    }
                }
            )

            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPropsDialog = true },
                headlineContent = {
                    Text(text = "添加更多道具", style = MaterialTheme.typography.titleMedium)
                },
                supportingContent = {
                    customOptions?.let { options ->
                        Text(text = "当前：${options.props.joinToString("、")}")
                    }
                }
            )

            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate("about")
                    },
                headlineContent = {
                    Text(text = "关于", style = MaterialTheme.typography.titleMedium)
                }
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }

    // 自定义心情弹窗
    CustomOptionsDialog(
        show = showMoodDialog,
        title = "心情",
        initialOptions = customOptions?.moods ?: emptyList(),
        onConfirm = { newMoods ->
            scope.launch {
                val newOptions = customOptions?.copy(moods = newMoods) ?: CustomOptions(moods = newMoods)
                SessionRepository.saveCustomOptions(context, newOptions)
                customOptions = newOptions
                Toast.makeText(context, "心情选项已保存", Toast.LENGTH_SHORT).show()
            }
            showMoodDialog = false
        },
        onDismiss = { showMoodDialog = false }
    )

    // 自定义道具弹窗
    CustomOptionsDialog(
        show = showPropsDialog,
        title = "道具",
        initialOptions = customOptions?.props ?: emptyList(),
        onConfirm = { newProps ->
            scope.launch {
                val newOptions = customOptions?.copy(props = newProps) ?: CustomOptions(props = newProps)
                SessionRepository.saveCustomOptions(context, newOptions)
                customOptions = newOptions
                Toast.makeText(context, "道具选项已保存", Toast.LENGTH_SHORT).show()
            }
            showPropsDialog = false
        },
        onDismiss = { showPropsDialog = false }
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        navController = rememberNavController()
    )
}
