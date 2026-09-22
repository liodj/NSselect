package com.example.nsselect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nsselect.ui.MainScreen
import com.example.nsselect.ui.SettingsScreen
import com.example.nsselect.ui.theme.NSselectTheme
import com.example.nsselect.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NSselectTheme {
                val viewModel: MainViewModel = viewModel()
                var selectedTab by remember { mutableStateOf(0) }
                val tabs = listOf("생성기", "설정 및 데이터")
                
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            tabs.forEachIndexed { index, title ->
                                NavigationBarItem(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    icon = {
                                        Icon(
                                            imageVector = if (index == 0) Icons.Default.List else Icons.Default.Settings,
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
                        if (selectedTab == 0) {
                            MainScreen(viewModel)
                        } else {
                            SettingsScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}