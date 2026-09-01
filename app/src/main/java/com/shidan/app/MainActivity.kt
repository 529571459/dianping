package com.shidan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shidan.app.ui.EditVisitScreen
import com.shidan.app.ui.LocalPalette
import com.shidan.app.ui.LogScreen
import com.shidan.app.ui.ShidanTheme
import com.shidan.app.ui.StatsScreen
import com.shidan.app.ui.WishScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Store.init(applicationContext)
        Store.sweepOrphanPhotos()
        setContent {
            ShidanTheme {
                AppRoot()
            }
        }
    }
}

private enum class Tab(val label: String, val glyph: String) {
    LOG("记录", "食"),
    WISH("想去", "想"),
    STATS("统计", "数")
}

@Composable
private fun AppRoot() {
    val palette = LocalPalette.current

    var tab by remember { mutableStateOf(Tab.LOG) }
    /** 不为 null 就是在编辑；id 为空字符串代表新建 */
    var editing by remember { mutableStateOf<Visit?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.paper)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    Tab.LOG -> LogScreen(
                        onNew = { editing = Visit(date = com.shidan.app.ui.todayIso()) },
                        onEdit = { editing = it }
                    )

                    Tab.WISH -> WishScreen(
                        onCook = { wishName ->
                            editing = Visit(name = wishName, date = com.shidan.app.ui.todayIso())
                        }
                    )

                    Tab.STATS -> StatsScreen()
                }
            }

            NavigationBar(
                containerColor = palette.card,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab.values().forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = {
                            Text(
                                text = t.glyph,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        label = { Text(t.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = palette.pine,
                            selectedTextColor = palette.ink,
                            unselectedIconColor = palette.inkSoft,
                            unselectedTextColor = palette.inkSoft,
                            indicatorColor = palette.card2
                        )
                    )
                }
            }
        }

        val target = editing
        if (target != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.paper)
                    .systemBarsPadding()
                    .imePadding()
                    .padding(bottom = 0.dp)
            ) {
                EditVisitScreen(
                    initial = target,
                    onClose = { editing = null },
                    onSaved = { editing = null }
                )
            }
        }
    }
}
