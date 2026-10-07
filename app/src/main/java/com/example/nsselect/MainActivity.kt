package com.example.nsselect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nsselect.ui.MainScreen
import com.example.nsselect.ui.DataScreen
import com.example.nsselect.ui.SettingsScreen
import com.example.nsselect.ui.theme.NSselectTheme
import com.example.nsselect.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NSselectTheme {
                val viewModel: MainViewModel = viewModel()
                var selectedTab by rememberSaveable { mutableStateOf(0) }
                val tabs = listOf("생성기", "데이터", "설정")
                val tabState = rememberSaveableStateHolder()
                
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            tabs.forEachIndexed { index, title ->
                                NavigationBarItem(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    icon = {
                                        Icon(
                                            imageVector = when (index) {
                                                0 -> Icons.AutoMirrored.Filled.List
                                                1 -> Icons.Default.TableChart
                                                else -> Icons.Default.Settings
                                            },
                                            contentDescription = title
                                        )
                                    },
                                    label = { Text(title) }
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    Surface(modifier = Modifier.padding(paddingValues)) {
                        tabState.SaveableStateProvider(selectedTab) {
                            when (selectedTab) {
                                0 -> MainScreen(viewModel)
                                1 -> DataScreen(viewModel)
                                else -> SettingsScreen(viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
